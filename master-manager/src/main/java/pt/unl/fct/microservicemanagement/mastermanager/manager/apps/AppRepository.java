/*
 * MIT License
 *
 * Copyright (c) 2020 usmanager
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package pt.unl.fct.microservicemanagement.mastermanager.manager.apps;

import pt.unl.fct.microservicemanagement.mastermanager.manager.services.ServiceOrder;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppRepository extends JpaRepository<AppEntity, Long> {

  @Query("select case when count(a) > 0 then true else false end "
      + "from AppEntity a "
      + "where lower(a.name) = lower(:appName)")
  boolean hasApp(@Param("appName") String appName);

  @Query("select new pt.unl.fct.microservicemanagement.mastermanager.manager.services"
      + ".ServiceOrder(s.service, s.launchOrder) "
      + "from AppEntity a join a.appServices s "
      + "where lower(a.name) = lower(:appName) order by s.launchOrder")
  List<ServiceOrder> getServicesOrder(@Param("appName") String appName);

  Optional<AppEntity> findByNameIgnoreCase(@Param("name") String name);

  @Query("select s "
      + "from AppEntity a join a.appServices s "
      + "where lower(a.name) = lower(:appName)")
  List<AppServiceEntity> getServices(@Param("appName") String appName);

}


