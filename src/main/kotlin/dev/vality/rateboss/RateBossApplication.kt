package dev.vality.rateboss

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class RateBossApplication

fun main(args: Array<String>) {
    runApplication<RateBossApplication>(*args)
}
