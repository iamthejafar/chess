import 'package:chess/dotenv.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:dio/dio.dart';

import '../../../services/api_client.dart';

class AuthRepository {
  final GoogleSignIn _googleSignIn = GoogleSignIn(
    clientId: clientId,
      scopes: ['email', 'https://www.googleapis.com/auth/contacts.readonly']);
  final ApiClient _apiClient = ApiClient();

  Future<String> signInWithGoogle() async {
    try {
      final GoogleSignInAccount? googleUser = await _googleSignIn.signIn();
      if (googleUser == null) {
        throw Exception('Sign in aborted by user.');
      }

      final GoogleSignInAuthentication googleAuth = await googleUser.authentication;

      final response = await _apiClient.post(
        'auth/google-signin',
        data: {
          'idToken': googleAuth.idToken!,
        },
      );

      if (response.statusCode == 200) {
        return googleUser.email;
      } else {
        throw Exception('Failed to authenticate with backend.');
      }
    } catch (e) {
      throw Exception(e.toString());
    }
  }


  Future<void> signInSilently() async {
    await _googleSignIn.signInSilently();
  }
}
