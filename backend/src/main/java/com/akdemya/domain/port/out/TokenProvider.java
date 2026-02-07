package com.akdemya.domain.port.out;

import java.util.Map;

public interface TokenProvider {
  String generate(String subject, Map<String, Object> claims);
}
