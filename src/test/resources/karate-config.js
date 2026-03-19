function fn() {
  var baseUrl = karate.properties['karate.baseUrl'] || 'http://localhost:8080';
  var users = {
    admin: {
      email: 'admin@admin.tus.com',
      password: 'Admin123',
      username: 'admin'
    },
    student: {
      email: 'student@student.tus.com',
      password: 'Student123',
      username: 'student'
    },
    invalidPassword: 'WrongPass'
  };
  return { baseUrl: baseUrl, users: users };
}
