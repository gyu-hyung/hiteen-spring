package kr.jiasoft.hiteen.config

import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator

@Configuration
class R2dbcTxConfig(
    private val connectionFactory: ConnectionFactory
) {

    @Bean
    fun txOperator(): TransactionalOperator =
        TransactionalOperator.create(
            R2dbcTransactionManager(connectionFactory)
        )
}
