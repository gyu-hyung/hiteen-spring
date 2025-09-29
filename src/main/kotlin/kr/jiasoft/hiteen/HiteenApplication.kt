package kr.jiasoft.hiteen

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class HiteenApplication



fun main(args: Array<String>) {
	runApplication<HiteenApplication>(*args)
}
