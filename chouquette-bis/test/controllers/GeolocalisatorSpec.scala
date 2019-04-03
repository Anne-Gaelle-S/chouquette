package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._

import play.core.server.Server
// import play.api.routing.sird._
import play.api.mvc._
import play.api.libs.ws._ 
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
// import play.api.http.Status._
import akka.stream.Materializer
import play.api.libs.concurrent.MaterializerProvider

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor.ActorSystem



class GeolocalisatorSpec extends PlaySpec with Injecting with GuiceOneAppPerTest {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val components = inject[ControllerComponents]

//   "A Geolocalisator" must {
//     "should return a JsonArray" in {
//         val myFuture = new Geolocalisator(stubControllerComponents())
//             .geolocalise()
//             .apply(FakeRequest())

//         // val result = Await.result(myFuture, 10 seconds)
//         val res  = myFuture.body.consumeData(new play.api.libs.concurrent.MaterializerProvider(ActorSystem()).get)
//         res.map{
//             case x => x.decodeString("UTF-8") mustBe JsArray("longitude et latitude")
//         }
    
//         res mustBe "a JsonArray"
//     }
//   }

    // object GeolocalisatorSpecTest extends Geolocalisator {
        
    // }

    // val controller = GeolocalisatorSpec
    // implicit lazy val geolocalisateur = inject[ControllerComponents]
    // val geolocalisateur = new Geolocalisator(stubControllerComponents())
     
    // val geolocalise = call(geolocalisateur.geolocalise(), fakeRequest)


    "Geolocalisator" should {
        "not return 404" when {
        "I go to the route /geolocalise" in {
            val result = route(app, FakeRequest(POST, "/geolocalise"))
            status(result.get) must not be NOT_FOUND
        }
        }
    }


    // "render the correct page with the geolocalised text" when {
    //   "I navigate to the page geolocalise" in {
    //     val result = geolocalisateur.geolocalise(((List("Pau","Toulouse","Lyon")).toString)).apply(FakeRequest(GET, "/geolocalise"))

    //     status(result) mustBe OK
    //     contentAsString(result) must include ("""
    //     {
    //         "places": [
    //             {
    //             "long": -0.37,
    //             "lat": 43.3
    //             },
    //             {
    //             "long": 1.444,
    //             "lat": 43.6045
    //             },
    //             {
    //             "long": 4.84,
    //             "lat": 45.76
    //             }
    //         ]
    //     }
    //     """)
    //     //arrange
    //     //action
    //     //assert
    //   }
    // }
//   }

"Geolocalise" should {
    "return Ok with a body containing a json with lat and long" in {

      // implicit val materializer = ActorMaterializer()(ActorSystem())

      val geolocalisateur = new Geolocalisator(components, mat = materializer)
      

      // val controller = new Geolocalisator()
      // val geolocalisateur = new Geolocalisator(stubControllerComponents())

    //   val request = FakeRequest("POST", "/geolocalise").withJsonBody(Json.parse(
    //     s"""{
    //         [
    //             "Pau",
    //             "Toulouse",
    //             "Lyon"
    //         ]
    //         }""".stripMargin))

    val request = FakeRequest("POST", "/user").withJsonBody(Json.parse(
        s"""["Pau","Toulouse","Lyon"]"""));

      val apiResult = call(geolocalisateur.geolocalise, request)

      status(apiResult) mustEqual OK

      val resultBody = contentAsJson(apiResult)

      resultBody mustEqual Json.parse(
        s"""{
          "places": [
            {
              "long": -0.37,
              "lat": 43.3
            },
            {
              "long": 1.444,
              "lat": 43.6045
            },
            {
              "long": 4.84,
              "lat": 45.76
            }
          ]
        }""")
    }
  }


//   "Geolocalisator POST" should {
//     "answer Ok" in {
//         implicit val materializer = ActorMaterializer()(ActorSystem())

//         val geolocalisateur = new Geolocalisator(stubControllerComponents())

//     //    val controllerComponents = 
//     //      Helpers.stubControllerComponents(
//     //        playBodyParsers = Helpers.stubPlayBodyParsers(materializer)
//     //      ) 

//     //    val controller = new HomeController(controllerComponents)

//        val fakeRequest = 
        //  FakeRequest(POST, "/json").withBody((List("Pau","Toulouse","Lyon")).toString)

        
//     //    val geolocalise = geolocalisateur.
//     //         geolocalise(fakeRequest)

//            val geolocalise = call(geolocalisateur.geolocalise(), fakeRequest)

//             // val home = call(controller.json(), fakeRequest)

//     //    status(geolocalise) mustBe OK("""[{"long":-0.37,"lat":43.3},{"long":1.444,"lat":43.6045},{"long":4.84,"lat":45.76}]""")
//        status(geolocalise) mustBe OK

//     }
//   }

}



