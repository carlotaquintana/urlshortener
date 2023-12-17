package es.unizar.urlshortener.core

class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class UnreachableUriException(message: String) : RuntimeException(message)

class InfoNotAvailable(key: String, msg: String) : Exception("[$key] [$msg] is not available yet")

class UrlNotSafe(val url: String) : Exception("[$url] is not safe")
