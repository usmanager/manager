/**
 * Registration-client-api
 * Interage com o registration server (eureka) para registar esta instância ou obter servidores com o qual pode comunicar
 *
 * OpenAPI spec version: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator 2.4.17.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

/*
 * JsonBody.h
 *
 * This is a JSON http body which can be submitted via http
 */

#ifndef IO_SWAGGER_CLIENT_MODEL_JsonBody_H_
#define IO_SWAGGER_CLIENT_MODEL_JsonBody_H_


#include "IHttpBody.h"

#include <cpprest/json.h>

namespace io {
namespace swagger {
namespace client {
namespace model {

class  JsonBody
    : public IHttpBody
{
public:
    JsonBody( const web::json::value& value );
    virtual ~JsonBody();

    void writeTo( std::ostream& target ) override;

protected:
    web::json::value m_Json;
};

}
}
}
}

#endif /* IO_SWAGGER_CLIENT_MODEL_JsonBody_H_ */