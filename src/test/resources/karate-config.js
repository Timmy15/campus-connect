function fn() {
  var baseUrl = karate.properties['karate.baseUrl'] || 'http://localhost:8080';
  return { baseUrl: baseUrl };
}
