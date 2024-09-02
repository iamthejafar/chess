import 'package:dio/dio.dart';

class ApiClient {
  final Dio _dio = Dio();

  ApiClient() {
    _dio.options.baseUrl = 'http://your-backend-server.com/api/';
    _dio.options.connectTimeout = const Duration(seconds: 5);
    _dio.options.receiveTimeout = const Duration(seconds: 5);
  }

  Future<Response> post(
      String endpoint, {
        required Map<String, dynamic> data,
      }) async {
    try {
      final response = await _dio.post(endpoint, data: data);
      return response;
    } on DioError catch (e) {
      throw Exception(e.response?.data['message'] ?? 'Failed to make API call.');
    }
  }
}
