/*
 * MIT License
 *
 * Copyright (c) 2020 manager
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

package nginx

import (
	"github.com/usmanager/manager/nginx-load-balancer-api/util/cmd"
	"github.com/usmanager/manager/nginx-load-balancer-api/util/files"
	"html/template"
	"log"
	"os"
	"path/filepath"
	"sync"

	"github.com/usmanager/manager/nginx-load-balancer-api/data"
)

const generatedPath = "generated"
const filePath = generatedPath + "/nginx.conf"
const dstPath = "/etc/nginx/nginx.conf"

const tmpl = `load_module "modules/ngx_http_geoip_module.so";
user nginx;
worker_processes auto;

error_log /dev/stdout info;
pid /var/run/nginx.pid;

events {
  worker_connections 1024;
}

http {
  geoip_city etc/nginx/geoip/city.dat;
  
  access_log /dev/stdout;

  upstream frontend {
	least_conn;
    {{- range $index, $server := .}}
    server {{$server.Hostname}} weight={{$server.Weight}};
    {{- end}}
  }

  server {
    listen 80;
    server_name load-balancer.com;

    include /etc/nginx/conf.d/*.conf;

    location / {
      proxy_connect_timeout 100;
      proxy_read_timeout 100;      

      proxy_pass http://frontend;  

      proxy_set_header X-Latitude $geoip_latitude;
      proxy_set_header x-Longitude $geoip_longitude;
	}

    location /_/nginx-load-balancer-api {
	  proxy_connect_timeout 100;
	  proxy_read_timeout 100;

	  auth_basic "Restricted";
	  auth_basic_user_file /etc/nginx/.htpasswd;           

	  proxy_pass http://localhost:1906;
	
	  proxy_set_header X-Forwarded-Host $host;
	  proxy_set_header Authorization "";
	  proxy_redirect off;
	}
  }
}
`

func UpdateNginx() {
	if len(data.Servers) > 0 {
		log.Print("Updating nginx config files")
		generateNginxConfigFile()
	}
}

func generateNginxConfigFile() {
	folderAbsPath, _ := filepath.Abs(generatedPath)
	fileAbsPath, _ := filepath.Abs(filePath)
	copyAbsPath, _ := filepath.Abs(dstPath)
	t := template.New("Nginx configuration file")
	t, err := t.Parse(tmpl)

	if err != nil {
		log.Fatal("Parse: ", err)
		return
	}

	if _, err := os.Stat(folderAbsPath); os.IsNotExist(err) {
		_ = os.Mkdir(folderAbsPath, 0755)
	}

	f, err := os.Create(fileAbsPath)
	if err != nil {
		log.Println("Create file: ", err)
		return
	}

	err = t.Execute(f, data.ServersWeight)
	if err != nil {
		log.Fatal("Execute: ", err)
		return
	}
	_ = f.Close()
	err = files.Copy(fileAbsPath, copyAbsPath)
	if err != nil {
		log.Println("Copying file failed ", err)
	} else {
		log.Println("Copying file succeeded")
		reloadNginx()
	}
}

func reloadNginx() {
	wg := new(sync.WaitGroup)
	wg.Add(1)
	cmd.Execute("nginx -s reload", wg)
	wg.Wait()
}
