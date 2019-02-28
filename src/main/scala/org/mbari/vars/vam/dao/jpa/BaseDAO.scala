/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.vars.vam.dao.jpa

import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.vam.dao.{ DAO, PersistentObject }
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag
import scala.reflect.classTag

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-05-06T11:18:00
 */
abstract class BaseDAO[B <: PersistentObject: ClassTag](val entityManager: EntityManager) extends DAO[B] {
  private[this] val log = LoggerFactory.getLogger(getClass)

  if (log.isInfoEnabled) {
    val props = entityManager.getProperties
    if (props.containsKey(BaseDAO.JDBC_URL_KEY)) {
      log.debug(s"Wrapping EntityManager with DAO for database: ${props.get(BaseDAO.JDBC_URL_KEY)}")
    }
  }

  def find(obj: B): Option[B] =
    obj.primaryKey.flatMap(pk => Option(entityManager.find(obj.getClass, pk)))

  def findByNamedQuery(name: String, namedParameters: Map[String, Any] = Map.empty): List[B] = {
    val query = entityManager.createNamedQuery(name)
    namedParameters.foreach({ case (a, b) => query.setParameter(a, b) })
    query.getResultList.asScala.toList.map(_.asInstanceOf[B])
  }

  def executeNamedQuery(name: String, namedParameters: Map[String, Any] = Map.empty): Unit = {
    val query = entityManager.createNamedQuery(name)
    namedParameters.foreach({ case (a, b) => query.setParameter(a, b) })
    query.executeUpdate()
  }

  /**
   * Lookup entity by primary key. A DAO will only return entities of their type.
   * Also, note that I had to use a little scala reflection magic here
   *
   * @param primaryKey
   * @return
   */
  //  override def findByUUID(primaryKey: UUID): Option[B] =
  //    Option(entityManager.find(classTag[B].runtimeClass, primaryKey).asInstanceOf[B])

  override def runTransaction[R](fn: this.type => R)(implicit ec: ExecutionContext): Future[R] = {
    import org.mbari.vars.vam.dao.jpa.Implicits.RichEntityManager
    def fn2(em: EntityManager): R = fn.apply(this)
    entityManager.runTransaction(fn2)
  }

  override def create(entity: B): Unit = entityManager.persist(entity)

  override def update(entity: B): B = entityManager.merge(entity)

  override def delete(entity: B): Unit = entityManager.remove(entity)

  def close(): Unit = if (entityManager.isOpen) entityManager.close()

}

object BaseDAO {
  val JDBC_URL_KEY = "javax.persistence.jdbc.url"
}