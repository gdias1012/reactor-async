package com.guilherme.reactor.async.controller

import models.pokemon.Pokemon
import models.pokemon.PokemonForm
import models.resource.NamedAPIResourceList
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux


@RestController
class TestController {

    private val client = WebClient.create("https://pokeapi.co/api/v2")

    private val template = RestTemplate()

    @GetMapping("async")
    fun async(@RequestParam limit: Long,
              @RequestParam offset: Long): Flux<Any> {
        return client.get()
                .uri("/pokemon/?limit=$limit&offset=$offset")
                .retrieve()
                .bodyToMono(NamedAPIResourceList::class.java)
                .flatMapIterable { it.results }
                .map {
                    client.get()
                            .uri(clear(it.url))
                            .retrieve()
                            .bodyToMono(Pokemon::class.java)
                }
                .flatMap { it }
                .map {
                     client.get()
                             .uri("/pokemon-form/${it.id}/")
                             .retrieve()
                             .bodyToMono(PokemonForm::class.java)
                }
                .flatMap { it }
                .map {
                    mapOf("name" to it.name,
                            "img" to it.sprites.frontDefault)
                }
    }

    @GetMapping("sync")
    fun sync(@RequestParam limit: Long,
             @RequestParam offset: Long): List<Map<String, Any>> {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36")
        val entity = HttpEntity("parameters", headers)


        val url = "https://pokeapi.co/api/v2/pokemon/?limit=$limit&offset=$offset"
        val results = template.exchange(url, HttpMethod.GET, entity, NamedAPIResourceList::class.java).body!!.results
        val pokemons = results.map {
            template.exchange(it.url, HttpMethod.GET, entity, Pokemon::class.java).body!!
        }
        val forms = pokemons.map {
            val url2 = "https://pokeapi.co/api/v2/pokemon-form/${it.id}/"
            template.exchange(url2, HttpMethod.GET, entity, Pokemon::class.java).body!!
        }
        return forms.map {
            mapOf("name" to it.name,
                    "img" to it.sprites.frontDefault)
        }
    }

    private fun clear(url: String): String = url.replace("https://pokeapi.co/api/v2", "")

}