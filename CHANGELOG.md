# act-ebean CHANGE LOG

1.3.1 - 11/Jan/2018
* update to act-1.6.2
* add `@BindOn` annotation to `EbeanConfigLoaded` event class to allow early bind of event listeners

1.3.0 - 28/Dec/2017 
* update to act-1.6.0

1.2.3
* update to act-1.4.11, act-sql-common-1.1.1
* apply oslg-bootstrap version mechanism
* improve maven build process

1.2.2
* catch up to sql-common-1.1.1

1.2.1
* catch up to ebean2-1.1.1

1.2.0
* catch up to act-1.4.0
* catch up to act-sql-common to 1.1.0

1.1.5
- NPE when no third party datasource configured #16 
- update sql-common to 1.0.2

1.1.4
- update to act-sql-common-1.0.1
- Ebean Agent loaded twice if there are two ebean db services #14 
- The datasource created in sql-common not used when creating ebean server #15 

1.1.3
- It should use ebean's naming convention by default #13 

1.1.2
- Migrate to act-1.1.0 new DB architecture #12 

1.1.1
- HikariDataSourceProvider.confKeyMapping error #10 
- DruidDataSourceProvider.confKeyMapping() error #11 

1.1.0
- Support plugin different datasource solution #9 
- change mysql jdbc driver class name #8 
- Support Druid database #6 

1.0.5
- It reports XXX table not found when start app in dev mode #7 

1.0.4
- take out version range from pom.xml. See https://issues.apache.org/jira/browse/MNG-3092

1.0.3
- version number of mistake

1.0.2
- use HikariCP for connection pool #4 
- Make it easy to do low level JDBC logic #5 

1.0.1
- EbeanDao.drop() method cause JdbcSQLException #1 
- EbeanInjectionListener not effect on User defined Dao #3 

1.0.0
- the first formal release

0.7.0 - update act to 0.7.0
0.6.0 - update act to 0.6.0
0.5.0 - upgrade to act 0.5.0 (to reserve 0.4.0 for techempower test)
0.4.0 - upgrade to act 0.4.0
0.3.1 - upgrade to act 0.3.1
0.3.0 - upgrade to act 0.3.0
0.2.0 - upgrade to act 0.2.0
      - upgrade to ebean-8
0.1.2 - upgrade to act 0.1.2
0.1.1 - baseline version
