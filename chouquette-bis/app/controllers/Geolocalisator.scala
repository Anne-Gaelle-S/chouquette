package controllers

import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import play.api._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.functional.syntax._ // Combinator syntax
import play.api.http.HttpEntity

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

// import models._

case class GeolocalisedCoordinates( places : List[String] )
// Exemple : List("http://fr.dbpedia.org/resource/Parsac", "http://fr.dbpedia.org/resource/Barbanne", "http://fr.dbpedia.org/resource/Forêt", "http://fr.dbpedia.org/resource/Alfred_de_Musset")

object GeolocalisedCoordinates {
  implicit val GeolocalisedCoordinatesReads = Json.reads[GeolocalisedCoordinates]
  implicit val GeolocalisedCoordinatesWrites = Json.writes[GeolocalisedCoordinates]
}

// case class Coords(long: Double, lat: Double)
// object Coords {
//   implicit val coordsReads: Reads[Coords] = (
//     (JsPath \ "long").read[Double] and
//     (JsPath \ "lat").read[Double]
//   )(Coords.apply _)
// }

class Geolocalisator @Inject() (
  ws: WSClient // for the http request
)(
  implicit ec: ExecutionContext // for the http response
) extends Controller {

    //Récupéré du service 2
    // val extractedText = List("http://fr.dbpedia.org/resource/Parsac", "http://fr.dbpedia.org/resource/Barbanne", "http://fr.dbpedia.org/resource/Forêt", "http://fr.dbpedia.org/resource/Alfred_de_Musset")

    // val url1 = "https://dbpedia.org/sparql?default-graph-uri=http://dbpedia.org&query=select ?place ?point where { ?place rdf:type dbo:Place . ?place rdfs:label ?nomPlace filter(str(?nomPlace)=\""
    // val url2 = "\") . ?place georss:point ?point} limit 1"


    val url1 = "https://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org&query=select+%3Fplace+%3Fpoint+where+%7B+%0D%0A%3Fplace+rdf%3Atype+dbo%3APlace+.+%0D%0A%3Fplace+rdfs%3Alabel+%3FnomPlace+filter%28str%28%3FnomPlace%29%3D%22"
    val url2 = "%22%29+.+%0D%0A%3Fplace+georss%3Apoint+%3Fpoint%0D%0A%7D+limit+1&format=application%2Fsparql-results%2Bjson&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on&run=+Run+Query+"
    

    // def listURL(extractedText: List[String]): List[String] = {
    //     val listURL = List()
    //     extractedText.foreach {
    //         place => {
    //             val placeSubstr = place.substring(
    //                 place.indexOf("http://fr.dbpedia.org/resource/")+("http://fr.dbpedia.org/resource/").length()
    //             )
    //             println(url1+placeSubstr+url2)
    //             val str = url1+placeSubstr+url2
    //             listURL :: List(str)
    //             println(listURL.length)
    //         }
    //     }
    //     return listURL
    // }

    // val listURL: List[String] = listURL(extractedText)

    // val listURL: List[String] = List(
    //     "https://dbpedia.org/sparql?default-graph-uri=http://dbpedia.org&query=select ?place ?point where { ?place rdf:type dbo:Place . ?place rdfs:label ?nomPlace filter(str(?nomPlace)=\"Parsac\") . ?place georss:point ?point} limit 1",
    //     "https://dbpedia.org/sparql?default-graph-uri=http://dbpedia.org&query=select ?place ?point where { ?place rdf:type dbo:Place . ?place rdfs:label ?nomPlace filter(str(?nomPlace)=\"Barbanne\") . ?place georss:point ?point} limit 1",
    //     "https://dbpedia.org/sparql?default-graph-uri=http://dbpedia.org&query=select ?place ?point where { ?place rdf:type dbo:Place . ?place rdfs:label ?nomPlace filter(str(?nomPlace)=\"Forêt\") . ?place georss:point ?point} limit 1",
    //     "https://dbpedia.org/sparql?default-graph-uri=http://dbpedia.org&query=select ?place ?point where { ?place rdf:type dbo:Place . ?place rdfs:label ?nomPlace filter(str(?nomPlace)=\"Alfred_de_Musset\") . ?place georss:point ?point} l"
    // )

    // println("plop")
    // println(listURL)

    def geolocaliseReq = Action(parse.json) {res => {

        var resultat:List[String] = List()

        println("POST REQUEST")
        println("res.body : " + res.body)
        val optionOfString = res.body.validate[Seq[String]].asOpt
        println("optionofString : " + optionOfString)
        // val optionOfCoords = res.body.validate[Seq[Coords]].asOpt
        // println(optionOfString.map(x => Ok(x.toString)))

        optionOfString.map( x => x.foreach { x =>
            println(x)
            
            
            ///// Géolocalise
            // Essayer de faire un ws.url sans que ça renvoie un Ok().
            ws
            .url(url1+x+url2)
            .get()
            .map(validator)
            .map {
                case Some(geolocalisedCoord) => {
                    val geolocalisedCoordJson = Json.parse(geolocalisedCoord)
                    // println(geolocalisedCoordJson)
                    val bindings = (geolocalisedCoordJson \ "results" \ "bindings").get
                    val coordinates = (bindings.head \ "point" \ "value").get
                    // println(coordinates)
                    // Ok( geolocalisedCoord.flatMap( coordinates => (coordinates \ "results" \ "bindings" \ "0" \ "point" \ "value") ).toString )
                    // println(coordinates.toString)

                    resultat = resultat ::: List(coordinates.toString)

                    println("--------" + resultat)

                    // println("TOSTRING : " + resultat.toString)

                    Ok(resultat.toString)

                }
                case None => NotFound("Not found")

            
        }


        }
        ).getOrElse(BadRequest("Mauvaise requete... "))

        Ok("Les coordonnnées ont été envoyées. ")
    }}



    def validator(response: WSResponse): Option[String] = {
        if(response.status == play.api.http.Status.OK) {
            // println(response.body)
            Some(response.body)
        } else {
            None
        }
        // else None

    } 
    def geolocalise(placeToGeolocalise: String) = Action.async {
        print("geolocalise ok")
        // val placeToGeolocaliseJson = Json.parse(placeToGeolocalise)
        // println(placeToGeolocalise)
        // println("fejofejfoejf :" + Json.stringify(placeToGeolocaliseJson))
        
        // println(placeToGeolocaliseJson.as[String])

        // val placeToGeolocaliseJson = placeToGeolocalise
        // val nameResult: JsResult[String] = (placeToGeolocaliseJson).validate[String]


        // val placeToGeolocaliseReads: Reads[String] = (JsPath \ "0").read[String]
        // val placeToGeolocaliseRes: JsResult[String] = placeToGeolocaliseJson.validate[String](placeToGeolocaliseReads)


        // placeToGeolocaliseRes match {
        //     case s: JsSuccess[String] => println("0: " + s.get)
        //     case e: JsError => println("Errors: " + JsError.toJson(e).toString())
        // }

        // val json: JsResult[String] = json.validate[String](placeToGeolocaliseReads)

        // json match {
        //     case s: JsSuccess[String] => println("0: " + s.get)
        //     case e: JsError => println("Errors: " + JsError.toJson(e).toString())
        // }

        // Pattern matching
        // placeToGeolocaliseJson match {
        // case JsSuccess(name, _) => println(s"0: 0")
        // case e: JsError => println(s"Errors: ${JsError toJson e}")
        // }
            
        
            // val placeToGeolocaliseJsonSubstr = placeToGeolocaliseJson(0).toString.substring(
            //     1,
            //     placeToGeolocaliseJson(0).toString.size-1
            // )
            // println(placeToGeolocaliseJsonSubstr)
         

        ws
        .url(url1+placeToGeolocalise+url2)
        .get()
        .map(validator)
        .map {
            case Some(geolocalisedCoord) => {
            val geolocalisedCoordJson = Json.parse(geolocalisedCoord)
            // println(geolocalisedCoordJson)
            val bindings = (geolocalisedCoordJson \ "results" \ "bindings").get
            val coordinates = (bindings.head \ "point" \ "value").get
            // println(coordinates)
            // Ok( geolocalisedCoord.flatMap( coordinates => (coordinates \ "results" \ "bindings" \ "0" \ "point" \ "value") ).toString )
            Ok (coordinates.toString)

            }
            case None => NotFound("Not found")


        }
    }

}


// Marche bien, mais pas de long/lat : 

// select ?place ?point where { 
// ?place rdf:type dbo:Place . 
// ?place rdfs:label ?nomPlace filter(str(?nomPlace)="Toulouse") . 
// ?place georss:point ?point
// } limit 1 



// Marche pas pour l'instant : 

// prefix prop-fr: <http://fr.dbpedia.org/property/>

// select ?place ?lat ?long where { 
// ?place rdf:type dbo:Place . 
// ?place rdfs:label ?nomPlace filter(str(?nomPlace)="Toulouse") . 
// ?place prop-fr:latitude ?lat .
// ?place prop-fr:longitude ?long
// } limit 1 


// https://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org&query=select+%3Fplace+%3Fpoint+where+%7B+%0D%0A%3Fplace+rdf%3Atype+dbo%3APlace+.+%0D%0A%3Fplace+rdfs%3Alabel+%3FnomPlace+filter%28str%28%3FnomPlace%29%3D%22Toulouse%22%29+.+%0D%0A%3Fplace+georss%3Apoint+%3Fpoint%0D%0A%7D+limit+1+&format=text%2Fhtml&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on&run=+Run+Query+


// ?default-graph-uri=http://dbpedia.org&query=select ?place ?point where { ?place rdf:type dbo:Place . ?place rdfs:label ?nomPlace filter(str(?nomPlace)="Toulouse") . ?place georss:point ?point} limit 1 