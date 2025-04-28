16/04
Today, I have been working most of time to fix the error handling for the Processed files, Files concerned @AsnDocumentProcessingStrategy.java @ProcessedFileServiceImpl.java @XmlProcessorServiceImpl.java, BUG FIXED

Else I have restructured the ASN_HEADERS table with all the necessary Fields check @ASN HEADERS STRUCTURE.csv, Guide @ASN_MAPPING_GUIDE.md 

Commited to github V1.2

Tomorrow I'm Planning To start testing the listner connector (Will start with SFTP) and restructure the ASN_LINES table

17/04

Today I have restructured the ASN LINES, then started stabilasing the listener starting with the SFTP protocol, faced a Critical issue related to the the configuration Layers (Client/Interface), since Listener is a seperate microservice in the backend, he have his own database on port 8081 and in the other hand the processor database is on the port 8080, so this will cause a heavy configuration process, meaning we would need to configure both Clients and interfaces through the unified frontend for both the processor and the listener, so decided to opte for a centrilazed configuration store in which we have added another microservice Shared-config in the backend that would help us using the entities that could be used by both Listener and Processor and we would have one Shared database between the 2 instead of 2, still the solution is not stable, I will carry on tomorrow, Mehdi OUT....

18/04

Today I have fully stabalized the centrillazed store in the sahred config with the postgresql, I have setup the database succefully,I have as well restructured the frontend, tomorrow I will fix some minor issues related to JPA/Hibernate maps Java types to database types, since the types in the entities (models in the shared config) doesn't match the database expectation, Mehdi OUT

23/04

It have been 4 days since I have logged the advancment of my project, the good news is that finally I have managed to stabalize the SFTP listner and it's synchronisation with RabbitMQ as a broker to the processor, so now we can easly configure our SFTP in the frontend depending from which server we will be consume the files from, by client and interface, then our backend listner uses this configuration to do multipule operation then send the message to the RabbitMQ, then the rabbitMQ through it queues and event it sends the message to the backedn processor for the final processing of the file and store the data in our database depending on the mapping rules already established in our frontend, and this is the reason why i couldn't log anything for the past 4 days, because it was a pain in the ass debugging a dozen of issues I face to implemnt and stabalize Phase 1, now Phase 1 done I will be moving to phase 2, Enhancing my Backend processor for a multiple interfaces handler because as of now the Middleware application is stable only for the ASN interface and that's 1 out of 6 Main interfaces, ORDER,BUSINESS PARTNERS,GOODSRECEIPT,SHIPCONFIRM and ITEM to GOOO....

Commited to github V1.2

-------------------------------------------------------------------------------------------

run in the background : cd backend/listener; mvn spring-boot:run
run in the background : cd backend/processor; mvn spring-boot:run
run in the background : cd frontend; npm start

docker volume rm deploy_postgres_data

run in the background : cd deploy; docker-compose up -d --build
run in the background cd deploy; docker-compose logs -f rabbitmq

docker volume rm deploy_postgres_data
docker-compose ps
docker-compose down

The configuration is consistent across all components. The only potential enhancement would be to add the dead-letter queue (inbound.processor.dlq) to the definitions file, as it's referenced in the listener's configuration but not explicitly defined in the RabbitMQ definitions.

Docker starts DB ➜ Empty Database Created ➜ Application Starts ➜ Flyway Runs Migration ➜ Schema Created

<flyway.version>9.20.0</flyway.version>



  <LGMNG>0.000</LGMNG>
  <LGMNG>50.000</LGMNG>


   FileUploadController
   └── FileUploadService
       └── AsnDocumentProcessingStrategy
           ├── ProcessedFileService
           ├── AsnService
           ├── MappingRuleService
           ├── XmlProcessor
           └── XmlValidationService


--------------------------------------------------------------------------------------------