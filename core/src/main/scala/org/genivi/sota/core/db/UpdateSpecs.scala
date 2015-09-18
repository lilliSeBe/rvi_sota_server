/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import eu.timepit.refined.Refined
import java.util.UUID
import org.genivi.sota.core.data.Package.Id
import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.core.data.UpdateSpec
import scala.collection.GenTraversable
import eu.timepit.refined.string.Uuid
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.data.{UpdateSpec, Download, Vehicle, UpdateStatus, Package}

object UpdateSpecs {

  import org.genivi.sota.refined.SlickRefined._
  import UpdateStatus._
  import SlickExtensions._

  implicit val UpdateStatusColumn = MappedColumnType.base[UpdateStatus, String]( _.value.toString, UpdateStatus.withName )

  class UpdateSpecsTable(tag: Tag) extends Table[(UUID, Vehicle.IdentificationNumber, UpdateStatus)](tag, "UpdateSpecs") {
    def requestId = column[UUID]("update_request_id")
    def vin = column[Vehicle.IdentificationNumber]("vin")
    def status = column[UpdateStatus]("status")

    def pk = primaryKey("pk_update_specs", (requestId, vin))

    def * = (requestId, vin, status)
  }

  class RequiredPackagesTable(tag: Tag) extends Table[(UUID, Vehicle.IdentificationNumber, Package.Name, Package.Version)](tag, "RequiredPackages") {
    def requestId = column[UUID]("update_request_id")
    def vin = column[Vehicle.IdentificationNumber]("vin")
    def packageName = column[Package.Name]("package_name")
    def packageVersion = column[Package.Version]("package_version")

    def pk = primaryKey("pk_downloads", (requestId, vin, packageName, packageVersion))

    def * = (requestId, vin, packageName, packageVersion)
  }

  val updateSpecs = TableQuery[UpdateSpecsTable]

  val requiredPackages = TableQuery[RequiredPackagesTable]

  def persist(updateSpec: UpdateSpec) : DBIO[Unit] = {
    val specProjection = (updateSpec.request.id, updateSpec.vin,  updateSpec.status)

    def dependencyProjection(p: Package) =
      (updateSpec.request.id, updateSpec.vin, p.id.name, p.id.version)

    DBIO.seq(
      updateSpecs += specProjection,
      requiredPackages ++= updateSpec.dependencies.map( dependencyProjection )
    )
  }

  def load( vin: Vehicle.IdentificationNumber, packageIds: Set[Package.Id] )
          (implicit ec: ExecutionContext) : DBIO[Iterable[UpdateSpec]] = {
    val requests = UpdateRequests.all.filter(r => r.packageName.mappedTo[String] ++ r.packageVersion.mappedTo[String] inSet packageIds.map( id => id.name.get + id.version.get ))
    val specs = updateSpecs.filter(_.vin === vin)
    val q = for {
      r  <- requests
      s  <- specs if (r.id === s.requestId)
      rp <- requiredPackages if (rp.vin === vin && rp.requestId === s.requestId)
      p  <- Packages.packages if (p.name === rp.packageName && p.version === rp.packageVersion)
    } yield (r, s.vin, s.status, p)
    q.result.map( _.groupBy(x => (x._1, x._2, x._3) ).map {
      case ((request, vin, status), xs) => UpdateSpec(request, vin, status, xs.map(_._4).toSet )
    })
  }

  def listUpdatesById(uuid: Refined[String, Uuid]): DBIO[Seq[(UUID, Vehicle.IdentificationNumber, UpdateStatus)]] =
    updateSpecs.filter(_.requestId === UUID.fromString(uuid.get)).result

}
