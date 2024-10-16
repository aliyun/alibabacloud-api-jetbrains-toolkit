package com.alibabacloud.api.service

import com.alibabacloud.models.telemetry.DefaultApplicationInfo
import com.aliyun.credentials.Client
import com.aliyun.tea.*
import com.aliyun.tea.TeaModel.validateParams
import com.aliyun.tea.interceptor.InterceptorChain
import com.aliyun.teautil.Common
import com.aliyun.teautil.models.RuntimeOptions
import java.io.InputStream


class OpenAPIClient(config: Config) {
    var _endpoint: String? = null
    var _regionId: String? = null
    var _protocol: String? = null
    var _method: String? = null
    var _userAgent: String? = null
    var _endpointRule: String? = null
    var _suffix: String? = null
    var _readTimeout: Int? = null
    var _connectTimeout: Int? = null
    var _httpProxy: String? = null
    var _httpsProxy: String? = null
    var _socks5Proxy: String? = null
    var _socks5NetWork: String? = null
    var _noProxy: String? = null
    var _network: String? = null
    var _maxIdleConns: Int? = null
    var _endpointType: String? = null
    var _credential: Client? = null
    var _signatureVersion: String? = null
    var _signatureAlgorithm: String? = null
    var _headers: Map<String, String?>? = null
    var _globalParameters: GlobalParameters? = null
    var _key: String? = null
    var _cert: String? = null
    var _ca: String? = null

    /**
     * Init client with Config
     * @param config config contains the necessary information to create a client
     */
    init {
        if (Common.isUnset(config)) {
            throw TeaException(
                TeaConverter.buildMap<Any?>(
                    TeaPair("code", "ParameterMissing"),
                    TeaPair("message", "'config' can not be unset"),
                ),
            )
        }
        if (!Common.empty(config.accessKeyId) && !Common.empty(config.accessKeySecret)) {
            if (!Common.empty(config.securityToken)) {
                config.type = "sts"
            } else {
                config.type = "access_key"
            }
            val credentialConfig = com.aliyun.credentials.models.Config.build(
                TeaConverter.buildMap<Any?>(
                    TeaPair("accessKeyId", config.accessKeyId),
                    TeaPair("type", config.type),
                    TeaPair("accessKeySecret", config.accessKeySecret),
                ),
            )
            credentialConfig.securityToken = config.securityToken
            _credential = Client(credentialConfig)
        } else if (!Common.isUnset(config.credential)) {
            _credential = config.credential
        }
        _endpoint = config.endpoint
        _endpointType = config.endpointType
        _network = config.network
        _suffix = config.suffix
        _protocol = config.protocol
        _method = config.method
        _regionId = config.regionId
        _userAgent = config.userAgent
        _readTimeout = config.readTimeout
        _connectTimeout = config.connectTimeout
        _httpProxy = config.httpProxy
        _httpsProxy = config.httpsProxy
        _noProxy = config.noProxy
        _socks5Proxy = config.socks5Proxy
        _socks5NetWork = config.socks5NetWork
        _maxIdleConns = config.maxIdleConns
        _signatureVersion = config.signatureVersion
        _signatureAlgorithm = config.signatureAlgorithm
        _globalParameters = config.globalParameters
        _key = config.key
        _cert = config.cert
        _ca = config.ca
    }

    /**
     * Encapsulate the request and invoke the network
     * @param action api name
     * @param version product version
     * @param protocol http or https
     * @param method e.g. GET
     * @param authType authorization type e.g. AK
     * @param bodyType response body type e.g. String
     * @param request object of OpenApiRequest
     * @param runtime which controls some details of call api, such as retry times
     * @return the response
     */
    @Throws(Exception::class)
    fun doRequest(params: Params, request: OpenApiRequest, runtime: RuntimeOptions): Map<String, *> {
        validateParams(params, "params")
        validateParams(request, "request")
        val runtime_ = TeaConverter.buildMap<Any>(
            TeaPair("timeouted", "retry"),
            TeaPair("key", Common.defaultString(runtime.key, _key)),
            TeaPair("cert", Common.defaultString(runtime.cert, _cert)),
            TeaPair("ca", Common.defaultString(runtime.ca, _ca)),
            TeaPair("readTimeout", Common.defaultNumber(runtime.readTimeout, _readTimeout)),
            TeaPair("connectTimeout", Common.defaultNumber(runtime.connectTimeout, _connectTimeout)),
            TeaPair("httpProxy", Common.defaultString(runtime.httpProxy, _httpProxy)),
            TeaPair("httpsProxy", Common.defaultString(runtime.httpsProxy, _httpsProxy)),
            TeaPair("noProxy", Common.defaultString(runtime.noProxy, _noProxy)),
            TeaPair("socks5Proxy", Common.defaultString(runtime.socks5Proxy, _socks5Proxy)),
            TeaPair("socks5NetWork", Common.defaultString(runtime.socks5NetWork, _socks5NetWork)),
            TeaPair("maxIdleConns", Common.defaultNumber(runtime.maxIdleConns, _maxIdleConns)),
            TeaPair(
                "retry",
                TeaConverter.buildMap<Any>(
                    TeaPair("retryable", runtime.autoretry),
                    TeaPair("maxAttempts", Common.defaultNumber(runtime.maxAttempts, 3)),
                ),
            ),
            TeaPair(
                "backoff",
                TeaConverter.buildMap<Any>(
                    TeaPair("policy", Common.defaultString(runtime.backoffPolicy, "no")),
                    TeaPair("period", Common.defaultNumber(runtime.backoffPeriod, 1)),
                ),
            ),
            TeaPair("ignoreSSL", runtime.ignoreSSL),
        )
        var _lastRequest: TeaRequest? = null
        var _lastException: Exception? = null
        val _now = System.currentTimeMillis()
        var _retryTimes = 0
        while (Tea.allowRetry(runtime_["retry"] as Map<String?, Any?>?, _retryTimes, _now)) {
            if (_retryTimes > 0) {
                val backoffTime = Tea.getBackoffTime(runtime_["backoff"], _retryTimes)
                if (backoffTime > 0) {
                    Tea.sleep(backoffTime)
                }
            }
            _retryTimes = _retryTimes + 1
            try {
                val request_ = TeaRequest()
                request_.protocol = Common.defaultString(_protocol, params.protocol)
                request_.method = params.method
                request_.pathname = params.pathname
                var globalQueries: Map<String, String> = HashMap()
                var globalHeaders: Map<String, String> = HashMap()
                if (!Common.isUnset(_globalParameters)) {
                    val globalParams = _globalParameters
                    if (!Common.isUnset(globalParams?.queries)) {
                        globalQueries = globalParams!!.queries!!
                    }
                    if (!Common.isUnset(globalParams?.headers)) {
                        globalHeaders = globalParams!!.headers!!
                    }
                }
                request_.query = TeaConverter.merge(
                    String::class.java,
                    globalQueries,
                    request.query,
                )
                // endpoint is setted in product client
                request_.headers = TeaConverter.merge(
                    String::class.java,
                    TeaConverter.buildMap<Any?>(
                        TeaPair("host", _endpoint),
                        TeaPair("x-acs-version", params.version),
                        TeaPair("x-acs-action", params.action),
                        TeaPair("user-agent", pluginUserAgent),
                        TeaPair("x-acs-date", com.aliyun.openapiutil.Client.getTimestamp()),
                        TeaPair("x-acs-signature-nonce", Common.getNonce()),
                        TeaPair("accept", "application/json"),
                    ),
                    globalHeaders,
                    request.headers,
                )
                if (Common.equalString(params.style, "RPC")) {
                    val headers = rpcHeaders
                    if (!Common.isUnset(headers)) {
                        request_.headers = TeaConverter.merge(
                            String::class.java,
                            request_.headers,
                            headers,
                        )
                    }
                }
                val signatureAlgorithm = Common.defaultString(_signatureAlgorithm, "ACS3-HMAC-SHA256")
                var hashedRequestPayload = com.aliyun.openapiutil.Client.hexEncode(
                    com.aliyun.openapiutil.Client.hash(
                        Common.toBytes(""),
                        signatureAlgorithm,
                    ),
                )
                if (!Common.isUnset(request.stream)) {
                    val tmp = Common.readAsBytes(request.stream)
                    hashedRequestPayload = com.aliyun.openapiutil.Client.hexEncode(
                        com.aliyun.openapiutil.Client.hash(
                            tmp,
                            signatureAlgorithm,
                        ),
                    )
                    request_.body = Tea.toReadable(tmp)
                    request_.headers["content-type"] = "application/octet-stream"
                } else {
                    if (!Common.isUnset(request.body)) {
                        if (Common.equalString(params.reqBodyType, "byte")) {
                            val byteObj = Common.assertAsBytes(request.body)
                            hashedRequestPayload = com.aliyun.openapiutil.Client.hexEncode(
                                com.aliyun.openapiutil.Client.hash(
                                    byteObj,
                                    signatureAlgorithm,
                                ),
                            )
                            request_.body = Tea.toReadable(byteObj)
                        } else if (Common.equalString(params.reqBodyType, "json")) {
                            val jsonObj = Common.toJSONString(request.body)
                            hashedRequestPayload = com.aliyun.openapiutil.Client.hexEncode(
                                com.aliyun.openapiutil.Client.hash(
                                    Common.toBytes(jsonObj),
                                    signatureAlgorithm,
                                ),
                            )
                            request_.body = Tea.toReadable(jsonObj)
                            request_.headers["content-type"] = "application/json; charset=utf-8"
                        } else {
                            val m = Common.assertAsMap(request.body)
                            val formObj = com.aliyun.openapiutil.Client.toForm(m)
                            hashedRequestPayload = com.aliyun.openapiutil.Client.hexEncode(
                                com.aliyun.openapiutil.Client.hash(
                                    Common.toBytes(formObj),
                                    signatureAlgorithm,
                                ),
                            )
                            request_.body = Tea.toReadable(formObj)
                            request_.headers["content-type"] = "application/x-www-form-urlencoded"
                        }
                    }
                }
                request_.headers["x-acs-content-sha256"] = hashedRequestPayload
                if (!Common.equalString(params.authType, "Anonymous")) {
                    val credentialModel = _credential!!.credential
                    val authType = credentialModel.type
                    if (Common.equalString(authType, "bearer")) {
                        val bearerToken = credentialModel.bearerToken
                        request_.headers["x-acs-bearer-token"] = bearerToken
                    } else {
                        val accessKeyId = credentialModel.accessKeyId
                        val accessKeySecret = credentialModel.accessKeySecret
                        val securityToken = credentialModel.securityToken
                        if (!Common.empty(securityToken)) {
                            request_.headers["x-acs-accesskey-id"] = accessKeyId
                            request_.headers["x-acs-security-token"] = securityToken
                        }
                        request_.headers["Authorization"] = com.aliyun.openapiutil.Client.getAuthorization(
                            request_,
                            signatureAlgorithm,
                            hashedRequestPayload,
                            accessKeyId,
                            accessKeySecret,
                        )
                    }
                }
                _lastRequest = request_
                val response_ = Tea.doAction(request_, runtime_, interceptorChain)

                val reqEntry = mutableMapOf<String, Any>()
                reqEntry["headers"] = request_.headers
                val respEntry = mutableMapOf<String, Any>()
                respEntry["statusCode"] = response_.statusCode
                respEntry["headers"] = response_.headers
                val entry = mutableMapOf<String, Any>()
                entry["request"] = reqEntry
                entry["response"] = respEntry

                val resp = mutableMapOf<String, Any>()
                if (Common.is4xx(response_.statusCode) || Common.is5xx(response_.statusCode)) {
                    var err: MutableMap<String?, Any?>
                    if (!Common.isUnset(response_.headers["content-type"]) && Common.equalString(
                            response_.headers["content-type"],
                            "text/xml;charset=utf-8",
                        )
                    ) {
                        val _str = Common.readAsString(response_.body)
                        val respMap = com.aliyun.teaxml.Client.parseXml(_str, null)
                        err = Common.assertAsMap(respMap["Error"])
                        Common.assertAsMap(respMap["Error"])
                        resp["format"] = "xml"
                    } // TODO application/xml
                    else {
                        val _res = Common.readAsJSON(response_.body)
                        err = Common.assertAsMap(_res)
                        resp["format"] = "json"
                    }
                    err["statusCode"] = response_.statusCode
                    resp["result"] = err
                    resp["entry"] = entry
                    return resp
                }
                if (Common.equalString(params.bodyType, "binary")) {
                    resp["format"] = "binary"
                    resp["result"] = response_.body
                    resp["entry"] = entry
                    return resp
                } else if (Common.equalString(params.bodyType, "byte")) {
                    val byt = Common.readAsBytes(response_.body)
                    resp["format"] = "byte"
                    resp["result"] = byt
                    resp["entry"] = entry
                    return resp
                } else if (Common.equalString(params.bodyType, "string")) {
                    val str = Common.readAsString(response_.body)
                    resp["format"] = "string"
                    resp["result"] = str
                    resp["entry"] = entry
                    return resp
                } else if (Common.equalString(params.bodyType, "json")) {
                    val obj = Common.readAsJSON(response_.body)
                    val res = Common.assertAsMap(obj)
                    resp["format"] = "json"
                    resp["result"] = res
                    resp["entry"] = entry
                    return resp
                } else if (Common.equalString(params.bodyType, "array")) {
                    val arr = Common.readAsJSON(response_.body)
                    resp["format"] = "array"
                    resp["result"] = arr
                    resp["entry"] = entry
                    return resp
                } else if (Common.equalString(params.bodyType, "xml")) {
                    resp["format"] = "xml"
                    resp["result"] = response_.body.toString()
                    resp["entry"] = entry
                    return resp
                } else {
                    val anything = Common.readAsString(response_.body)
                    resp["format"] = "anything"
                    resp["result"] = anything
                    resp["entry"] = entry
                    return resp
                }
            } catch (e: Exception) {
                if (Tea.isRetryable(e)) {
                    _lastException = e
                    continue
                }
                throw e
            }
        }
        throw TeaUnretryableException(_lastRequest, _lastException)
    }

    @get:Throws(Exception::class)
    val userAgent: String
        /**
         * Get user agent
         * @return user agent
         */
        get() = Common.getUserAgent(_userAgent)

    @get:Throws(Exception::class)
    val pluginUserAgent: String
        /**
         * Get user agent of plugin
         * @return user agent
         */
        get() = DefaultApplicationInfo.userAgent

    @get:Throws(Exception::class)
    val accessKeyId: String
        /**
         * Get accesskey id by using credential
         * @return accesskey id
         */
        get() = if (Common.isUnset(_credential)) {
            ""
        } else {
            _credential!!.accessKeyId
        }

    @get:Throws(Exception::class)
    val accessKeySecret: String
        /**
         * Get accesskey secret by using credential
         * @return accesskey secret
         */
        get() {
            return if (Common.isUnset(_credential)) {
                ""
            } else {
                _credential!!.accessKeySecret
            }
        }

    @get:Throws(Exception::class)
    val securityToken: String
        /**
         * Get security token by using credential
         * @return security token
         */
        get() {
            return if (Common.isUnset(_credential)) {
                ""
            } else {
                _credential!!.securityToken
            }
        }

    @get:Throws(Exception::class)
    val bearerToken: String
        /**
         * Get bearer token by credential
         * @return bearer token
         */
        get() {
            return if (Common.isUnset(_credential)) {
                ""
            } else {
                _credential!!.bearerToken
            }
        }

    @get:Throws(Exception::class)
    val type: String
        /**
         * Get credential type by credential
         * @return credential type e.g. access_key
         */
        get() {
            return if (Common.isUnset(_credential)) {
                ""
            } else {
                _credential!!.type
            }
        }

    /**
     * If the endpointRule and config.endpoint are empty, throw error
     * @param config config contains the necessary information to create a client
     */
    @Throws(Exception::class)
    fun checkConfig(config: Config) {
        if (Common.empty(_endpointRule) && Common.empty(config.endpoint)) {
            throw TeaException(
                TeaConverter.buildMap<Any?>(
                    TeaPair("code", "ParameterMissing"),
                    TeaPair("message", "'config.endpoint' can not be empty"),
                ),
            )
        }
    }

    @get:Throws(Exception::class)
    @set:Throws(Exception::class)
    var rpcHeaders: Map<String, String?>?
        /**
         * get RPC header for debug
         */
        get() {
            val headers = _headers
            _headers = null
            return headers
        }
        /**
         * set RPC header for debug
         * @param headers headers for debug, this header can be used only once.
         */
        set(headers) {
            _headers = headers
        }

    companion object {
        private val interceptorChain = InterceptorChain.create()

        /**
         * If inputValue is not null, return it or return defaultValue
         * @param inputValue  users input value
         * @param defaultValue default value
         * @return the final result
         */
        @Throws(Exception::class)
        fun defaultAny(inputValue: Any?, defaultValue: Any?): Any? {
            return if (Common.isUnset(inputValue)) {
                defaultValue
            } else {
                inputValue
            }
        }
    }

    class Params : TeaModel() {
        @NameInMap("action")
        @Validation(required = true)
        var action: String? = null

        @NameInMap("version")
        @Validation(required = true)
        var version: String? = null

        @NameInMap("protocol")
        @Validation(required = true)
        var protocol: String? = null

        @NameInMap("pathname")
        @Validation(required = true)
        var pathname: String? = null

        @NameInMap("method")
        @Validation(required = true)
        var method: String? = null

        @NameInMap("authType")
        @Validation(required = true)
        var authType: String? = null

        @NameInMap("bodyType")
        @Validation(required = true)
        var bodyType: String? = null

        @NameInMap("reqBodyType")
        @Validation(required = true)
        var reqBodyType: String? = null

        @NameInMap("style")
        var style: String? = null

        fun setAction(action: String?): Params {
            this.action = action
            return this
        }

        fun setVersion(version: String?): Params {
            this.version = version
            return this
        }

        fun setProtocol(protocol: String?): Params {
            this.protocol = protocol
            return this
        }

        fun setPathname(pathname: String?): Params {
            this.pathname = pathname
            return this
        }

        fun setMethod(method: String?): Params {
            this.method = method
            return this
        }

        fun setAuthType(authType: String?): Params {
            this.authType = authType
            return this
        }

        fun setBodyType(bodyType: String?): Params {
            this.bodyType = bodyType
            return this
        }

        fun setReqBodyType(reqBodyType: String?): Params {
            this.reqBodyType = reqBodyType
            return this
        }

        fun setStyle(style: String?): Params {
            this.style = style
            return this
        }

        companion object {
            @Throws(java.lang.Exception::class)
            fun build(map: Map<String?, *>?): Params {
                val self = Params()
                return build(map, self)
            }
        }
    }

    class OpenApiRequest : TeaModel() {
        @NameInMap("headers")
        var headers: Map<String, String>? = null

        @NameInMap("query")
        var query: Map<String, String>? = null

        @NameInMap("body")
        var body: Any? = null

        @NameInMap("stream")
        var stream: InputStream? = null

        @NameInMap("hostMap")
        var hostMap: Map<String, String>? = null

        @NameInMap("endpointOverride")
        var endpointOverride: String? = null

        fun setHeaders(headers: Map<String, String>?): OpenApiRequest {
            this.headers = headers
            return this
        }

        fun setQuery(query: Map<String, String>?): OpenApiRequest {
            this.query = query
            return this
        }

        fun setBody(body: Any?): OpenApiRequest {
            this.body = body
            return this
        }

        fun setStream(stream: InputStream?): OpenApiRequest {
            this.stream = stream
            return this
        }

        fun setHostMap(hostMap: Map<String, String>?): OpenApiRequest {
            this.hostMap = hostMap
            return this
        }

        fun setEndpointOverride(endpointOverride: String?): OpenApiRequest {
            this.endpointOverride = endpointOverride
            return this
        }

        companion object {
            @Throws(java.lang.Exception::class)
            fun build(map: Map<String?, *>?): OpenApiRequest {
                val self = OpenApiRequest()
                return build(map, self)
            }
        }
    }


    class GlobalParameters : TeaModel() {
        @NameInMap("headers")
        var headers: Map<String, String>? = null

        @NameInMap("queries")
        var queries: Map<String, String>? = null

        fun setHeaders(headers: Map<String, String>?): GlobalParameters {
            this.headers = headers
            return this
        }

        fun setQueries(queries: Map<String, String>?): GlobalParameters {
            this.queries = queries
            return this
        }

        companion object {
            @Throws(java.lang.Exception::class)
            fun build(map: Map<String?, *>?): GlobalParameters {
                val self = GlobalParameters()
                return build(map, self)
            }
        }
    }


    class Config : TeaModel() {
        /**
         *
         * accesskey id
         */
        @NameInMap("accessKeyId")
        var accessKeyId: String? = null

        /**
         *
         * accesskey secret
         */
        @NameInMap("accessKeySecret")
        var accessKeySecret: String? = null

        /**
         *
         * security token
         */
        @NameInMap("securityToken")
        var securityToken: String? = null

        /**
         *
         * http protocol
         */
        @NameInMap("protocol")
        var protocol: String? = null

        /**
         *
         * http method
         */
        @NameInMap("method")
        var method: String? = null

        /**
         *
         * region id
         */
        @NameInMap("regionId")
        var regionId: String? = null

        /**
         *
         * read timeout
         */
        @NameInMap("readTimeout")
        var readTimeout: Int? = null

        /**
         *
         * connect timeout
         */
        @NameInMap("connectTimeout")
        var connectTimeout: Int? = null

        /**
         *
         * http proxy
         */
        @NameInMap("httpProxy")
        var httpProxy: String? = null

        /**
         *
         * https proxy
         */
        @NameInMap("httpsProxy")
        var httpsProxy: String? = null

        /**
         *
         * credential
         */
        @NameInMap("credential")
        var credential: Client? = null

        /**
         *
         * endpoint
         */
        @NameInMap("endpoint")
        var endpoint: String? = null

        /**
         *
         * proxy white list
         */
        @NameInMap("noProxy")
        var noProxy: String? = null

        /**
         *
         * max idle conns
         */
        @NameInMap("maxIdleConns")
        var maxIdleConns: Int? = null

        /**
         *
         * network for endpoint
         */
        @NameInMap("network")
        var network: String? = null

        /**
         *
         * user agent
         */
        @NameInMap("userAgent")
        var userAgent: String? = null

        /**
         *
         * suffix for endpoint
         */
        @NameInMap("suffix")
        var suffix: String? = null

        /**
         *
         * socks5 proxy
         */
        @NameInMap("socks5Proxy")
        var socks5Proxy: String? = null

        /**
         *
         * socks5 network
         */
        @NameInMap("socks5NetWork")
        var socks5NetWork: String? = null

        /**
         *
         * endpoint type
         */
        @NameInMap("endpointType")
        var endpointType: String? = null

        /**
         *
         * OpenPlatform endpoint
         */
        @NameInMap("openPlatformEndpoint")
        var openPlatformEndpoint: String? = null

        /**
         *
         * credential type
         */
        @NameInMap("type")
//        @Deprecated("")
        var type: String? = null

        /**
         *
         * Signature Version
         */
        @NameInMap("signatureVersion")
        var signatureVersion: String? = null

        /**
         *
         * Signature Algorithm
         */
        @NameInMap("signatureAlgorithm")
        var signatureAlgorithm: String? = null

        /**
         *
         * Global Parameters
         */
        @NameInMap("globalParameters")
        var globalParameters: GlobalParameters? = null

        /**
         *
         * privite key for client certificate
         */
        @NameInMap("key")
        var key: String? = null

        /**
         *
         * client certificate
         */
        @NameInMap("cert")
        var cert: String? = null

        /**
         *
         * server certificate
         */
        @NameInMap("ca")
        var ca: String? = null

        fun setAccessKeyId(accessKeyId: String?): Config {
            this.accessKeyId = accessKeyId
            return this
        }

        fun setAccessKeySecret(accessKeySecret: String?): Config {
            this.accessKeySecret = accessKeySecret
            return this
        }

        fun setSecurityToken(securityToken: String?): Config {
            this.securityToken = securityToken
            return this
        }

        fun setProtocol(protocol: String?): Config {
            this.protocol = protocol
            return this
        }

        fun setMethod(method: String?): Config {
            this.method = method
            return this
        }

        fun setRegionId(regionId: String?): Config {
            this.regionId = regionId
            return this
        }

        fun setReadTimeout(readTimeout: Int?): Config {
            this.readTimeout = readTimeout
            return this
        }

        fun setConnectTimeout(connectTimeout: Int?): Config {
            this.connectTimeout = connectTimeout
            return this
        }

        fun setHttpProxy(httpProxy: String?): Config {
            this.httpProxy = httpProxy
            return this
        }

        fun setHttpsProxy(httpsProxy: String?): Config {
            this.httpsProxy = httpsProxy
            return this
        }

        fun setCredential(credential: Client?): Config {
            this.credential = credential
            return this
        }

        fun setEndpoint(endpoint: String?): Config {
            this.endpoint = endpoint
            return this
        }

        fun setNoProxy(noProxy: String?): Config {
            this.noProxy = noProxy
            return this
        }

        fun setMaxIdleConns(maxIdleConns: Int?): Config {
            this.maxIdleConns = maxIdleConns
            return this
        }

        fun setNetwork(network: String?): Config {
            this.network = network
            return this
        }

        fun setUserAgent(userAgent: String?): Config {
            this.userAgent = userAgent
            return this
        }

        fun setSuffix(suffix: String?): Config {
            this.suffix = suffix
            return this
        }

        fun setSocks5Proxy(socks5Proxy: String?): Config {
            this.socks5Proxy = socks5Proxy
            return this
        }

        fun setSocks5NetWork(socks5NetWork: String?): Config {
            this.socks5NetWork = socks5NetWork
            return this
        }

        fun setEndpointType(endpointType: String?): Config {
            this.endpointType = endpointType
            return this
        }

        fun setOpenPlatformEndpoint(openPlatformEndpoint: String?): Config {
            this.openPlatformEndpoint = openPlatformEndpoint
            return this
        }

        fun setType(type: String?): Config {
            this.type = type
            return this
        }

        fun setSignatureVersion(signatureVersion: String?): Config {
            this.signatureVersion = signatureVersion
            return this
        }

        fun setSignatureAlgorithm(signatureAlgorithm: String?): Config {
            this.signatureAlgorithm = signatureAlgorithm
            return this
        }

        fun setGlobalParameters(globalParameters: GlobalParameters?): Config {
            this.globalParameters = globalParameters
            return this
        }

        fun setKey(key: String?): Config {
            this.key = key
            return this
        }

        fun setCert(cert: String?): Config {
            this.cert = cert
            return this
        }

        fun setCa(ca: String?): Config {
            this.ca = ca
            return this
        }

        companion object {
            @Throws(java.lang.Exception::class)
            fun build(map: Map<String?, *>?): Config {
                val self = Config()
                return build(map, self)
            }
        }
    }
}
