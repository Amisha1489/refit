package edu.cdl.iot.integrations.notebook.camel.dependencies

import edu.cdl.iot.common.config.RefitConfig
import edu.cdl.iot.common.security.EncryptionHelper
import edu.cdl.iot.data.cassandra.CassandraRepository
import edu.cdl.iot.data.kafka.KafkaRepository
import edu.cdl.iot.data.minio.MinioRepository
import edu.cdl.iot.integrations.notebook.camel.routes.{NotebookImportRoutes, NotebookModelRoutes, NotebookProjectRoutes, NotebookQueryRoutes}
import edu.cdl.iot.integrations.notebook.cassandra.repository.{NotebookCassandraOrganizationRepository, NotebookCassandraProjectRepository, NotebookCassandraSensorDataRepository, NotebookCassandraSensorRepository, NotebookCassandraTrainingWindowRepository}
import edu.cdl.iot.integrations.notebook.core.service.{NotebookImportService, NotebookModelService, NotebookProjectService, NotebookQueryService, NotebookUploadService}
import edu.cdl.iot.integrations.notebook.kafka.repository.{NotebookKafkaImportRepository, NotebookKafkaModelRepository, NotebookKafkaStaticDataImportRepository, NotebookKafkaTrainingWindowImportRepository}
import edu.cdl.iot.integrations.notebook.minio.repository.{NotebookMinioFileImportRepository, NotebookMinioFileUploadRepository, NotebookMinioSchemaRepository}
import edu.cdl.iot.notebook.jdbi.dependencies.NotebookJdbiDependencies
import org.apache.camel.CamelContext
import org.jdbi.v3.core.Jdbi

class NotebookDependencies(config: RefitConfig,
                           cassandraRepository: CassandraRepository,
                           kafkaRepository: KafkaRepository,
                           minioRepository: MinioRepository,
                           jdbi: Jdbi,
                           camelContext: CamelContext,
                           decryptionHelperProvider: String => EncryptionHelper) {
  private val projectRepository = new NotebookCassandraProjectRepository(cassandraRepository)
  private val organizationRepository = new NotebookCassandraOrganizationRepository(cassandraRepository)
  private val sensorRepository = new NotebookCassandraSensorRepository(cassandraRepository)
  private val sensorDataRepository = new NotebookCassandraSensorDataRepository(
    cassandraRepository = cassandraRepository,
    decryptionHelper = decryptionHelperProvider
  )
  private val trainingWindowRepository = new NotebookCassandraTrainingWindowRepository(cassandraRepository)
  private val importRepository = new NotebookKafkaImportRepository(kafkaRepository)
  private val trainingWindowImportRepository = new NotebookKafkaTrainingWindowImportRepository(kafkaRepository)
  private val staticDataImportRepository = new NotebookKafkaStaticDataImportRepository(kafkaRepository)
  private val schemaRepository = new NotebookMinioSchemaRepository(minioRepository)
  private val modelRepository = new NotebookKafkaModelRepository(kafkaRepository)
  private val importFileRepository = new NotebookMinioFileImportRepository(minioRepository)
  private val uploadFileRepository = new NotebookMinioFileUploadRepository(minioRepository)
  private val jdbiDependencies = new NotebookJdbiDependencies(jdbi)


  private val importService = new NotebookImportService(
    minioConfig = config.getMinioConfig(),
    fileRepository = importFileRepository,
    projectRepository = projectRepository,
    trainingWindowRepository = trainingWindowRepository,
    importRepository = importRepository,
    trainingWindowImportRepository = trainingWindowImportRepository,
    staticDataImportRepository = staticDataImportRepository,
    staticDataRepository = null,
    sensorDataRepository = null
  )

  private val uploadService = new NotebookUploadService(
    fileRepository = uploadFileRepository
  )

  private val queryService = new NotebookQueryService(
    projectRepository = projectRepository,
    sensorRepository = sensorRepository,
    sensorDataRepository = sensorDataRepository,
    trainingWindowRepository = trainingWindowRepository
  )

  private val modelService = new NotebookModelService(
    projectRepository = projectRepository,
    modelRepository = modelRepository
  )

  private val projectService = new NotebookProjectService(
    projectRepository = projectRepository,
    schemaRepository = schemaRepository,
    organizationRepository = organizationRepository,
    dataSourceRepository = jdbiDependencies.dataSourceRepository
  )

  val queryRoutes =new NotebookQueryRoutes(camelContext, uploadService, queryService)
  val importRoutes = new NotebookImportRoutes(camelContext, importService)
  val projectRoutes = new NotebookProjectRoutes(camelContext, projectService)
  val modelRoutes = new NotebookModelRoutes(camelContext, modelService)

}
