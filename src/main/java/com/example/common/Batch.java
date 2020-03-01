package com.example.common;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.example.domain.Arashi;
import com.example.domain.Item;
@Transactional
@Configuration //付与することでSpring Batch設定用のクラスとなる
@EnableBatchProcessing //付与することでSpring Batch設定用のクラスとなる
public class Batch {
	
	@Autowired
	public JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	public StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	public DataSource dataSource;
	
	/**
	 * csvファイル読み込み
	 * @return
	 */
	@Bean
	public FlatFileItemReader<Arashi> reader(){
		FlatFileItemReader<Arashi> reader = new FlatFileItemReader<Arashi>();
//		reader.setEncoding("SJIS");
		reader.setResource(new ClassPathResource("arashi.csv")); //readerにcsvファイル読み込み
		reader.setLineMapper(new DefaultLineMapper<Arashi>() {
			{ //53~58行目で後術するSQLに読み込ませる配列を設定し、Arashiクラスと紐付ける
			      setLineTokenizer(new DelimitedLineTokenizer() {
			    	  {
				           setNames(new String[] {"id", "name", "day", "email", "companyId"});
			          } 
			});
			setFieldSetMapper(new BeanWrapperFieldSetMapper<Arashi>() {{
				setTargetType(Arashi.class);
			}});
		}});
		return reader;
	}
	
	//Processorで読み込んだ物を加工
//	@Bean
//	public ArashiProcessor processor() {
//		return new ArashiProcessor();
//	}
	
	//Writerで加工したデータを書き込む
	//DBへcsvの読み込んだ内容をINSERT
	@Bean
	public JdbcBatchItemWriter<Arashi> writer(){
		JdbcBatchItemWriter<Arashi> writer = new JdbcBatchItemWriter<Arashi>();
		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Arashi>());
		writer.setSql("INSERT INTO arashi(id,name,day,email,companyId) VALUES (:id, :name, :day, :email, :companyId)"); //55行目で設定した物を76行目のSQLで読み込ませるので合わせる
		writer.setDataSource(dataSource);
		return writer;
	}
	
	////////////////////////////////////////////////////////////
	
	private static final String itemSql = "SELECT id, name, price FROM items ORDER BY id";
	/**
	 * DBからitemsテーブルの中身を取得
	 * @return
	 */
	@Bean
	public ItemReader<Item> readerDB(){
		JdbcCursorItemReader<Item> readerDB = new JdbcCursorItemReader<>();
		readerDB.setDataSource(dataSource);
		readerDB.setSql(itemSql);
		readerDB.setRowMapper(new BeanPropertyRowMapper<>(Item.class));
		
		return readerDB;
	}
	/**
	 * DBから取得したitems情報をcsvファイルへ書き出す
	 * @return
	 */
	@Bean
	public FlatFileItemWriter<Item> writerCSV(){
		FlatFileItemWriter<Item> writerCSV = new FlatFileItemWriter<Item>();
		writerCSV.setResource(new FileSystemResource("/Users/yamadeayaka/item.csv"));
		writerCSV.setEncoding("SJIS");
		writerCSV.setAppendAllowed(true);
		writerCSV.setLineAggregator(new ItemAggregator());
		
		return writerCSV;
	}
	
	//Jobの開始と終了を案内するJobStartEndListenerを返す
	//DB情報をセット
	@Bean
	public JobExecutionListener listener() {
		return new JobStartEndListener(new JdbcTemplate(dataSource));
	}
	
	//ステップ1
	//取り込んだcsvデータを取得し、DBへINSERT
	@Bean
	public Step step1() {
		return stepBuilderFactory.get("step1")
		.<Arashi,Arashi> chunk(10)
		.reader(reader())
//		.processor(processor())
		.writer(writer())
		.build();
	}
	
	//ステップ2
	//DBから情報を取得し、csvファイルへ書き込む
	@Bean
	public Step step2() {
		return stepBuilderFactory.get("step2")
		.<Arashi,Arashi> chunk(10)
		.reader(reader())
//		.processor(processor())
		.writer(writer())
		.build();
	}
	
	//ここでListenerとStepを読み込ませる
	@Bean
	public Job job() {
		return jobBuilderFactory.get("job")
		.incrementer(new RunIdIncrementer())
		.listener(listener())
		.flow(step1())
		.next(step2())
		.end()
		.build();
	}

}
