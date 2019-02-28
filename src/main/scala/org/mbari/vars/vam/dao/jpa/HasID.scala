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

import com.google.gson.annotations.Expose
import javax.persistence.{ Column, GeneratedValue, GenerationType, Id }
import org.mbari.vars.vam.dao.PersistentObject

/**
 * @author Brian Schlining
 * @since 2019-02-28T10:15:00
 */
trait HasID extends PersistentObject {

  @Expose(serialize = false)
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  var id: Long = _

  override def primaryKey: Option[Long] = Option(id)
}
