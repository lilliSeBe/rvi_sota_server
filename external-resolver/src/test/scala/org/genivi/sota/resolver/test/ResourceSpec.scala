/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{Uri, HttpRequest}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.flywaydb.core.Flyway
import org.genivi.sota.resolver.db.{Vehicles, Packages}
import org.genivi.sota.resolver.types.{Vehicle$, Package}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Suite, WordSpec, PropSpec, Matchers}
import slick.jdbc.JdbcBackend.Database


trait ResourceSpec extends Matchers
    with ScalatestRouteTest
    with BeforeAndAfterAll { self: Suite =>

  // URIs
  val BasePath = Path("/api") / "v1"
  val VehiclesUri           = (vin: String) => resourceUri("vehicles", vin)
  val ResolveUri            = (name: String, version: String) => resourceUri("resolve", name, version)
  val FiltersUri            = resourceUri("filters")
  val ValidateUri           = (s: String) => resourceUri("validate", s)
  val PackageFiltersUri     = resourceUri("packageFilters")

  def PackageFiltersListUri(ss: String*) =
    resourceUri(("packageFilters" +: ss): _*)

  def resourceUri(pathSuffixes: String*): Uri =
    Uri.Empty.withPath(pathSuffixes.foldLeft(BasePath)(_/_))

  // Operations on the resources.
  def addPackage
    (name: String, version: String, desc: Option[String], vendor: Option[String])
      : HttpRequest
  = Put(resourceUri("packages", name, version), Package.Metadata(desc, vendor))

  // Database
  val name = "test-database"
  val db = Database.forConfig(name)

  override def beforeAll() = {
    val dbConfig = system.settings.config.getConfig(name)
    val url      = dbConfig.getString("url")
    val user     = dbConfig.getConfig("properties").getString("user")
    val passwd   = dbConfig.getConfig("properties").getString("password")

    val flyway = new Flyway
    flyway.setDataSource(url, user, passwd)
    flyway.setLocations("classpath:db.migration")
    flyway.clean()
    flyway.migrate()
  }

  override def afterAll() = {
    system.shutdown()
    db.close()
  }

  // Route
  lazy val route = new org.genivi.sota.resolver.Routing(db).route
}

trait ResourceWordSpec extends WordSpec with ResourceSpec
trait ResourcePropSpec extends PropSpec with ResourceSpec with PropertyChecks
