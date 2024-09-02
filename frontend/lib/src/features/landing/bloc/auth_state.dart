part of 'auth_bloc.dart';

@immutable
sealed class AuthState {}

class AuthInitial extends AuthState {}

class AuthLoading extends AuthState {}

class AuthAuthenticated extends AuthState {
  final String userEmail;
  AuthAuthenticated({required this.userEmail});
}

class AuthError extends AuthState {
  final String error;
  AuthError({required this.error});
}