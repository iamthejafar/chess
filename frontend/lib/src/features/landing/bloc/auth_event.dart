part of 'auth_bloc.dart';

@immutable
sealed class AuthEvent {}

class GoogleSignInRequested extends AuthEvent {}

class InitAuth extends AuthEvent {}

