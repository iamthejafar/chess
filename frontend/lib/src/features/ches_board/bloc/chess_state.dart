part of 'chess_bloc.dart';

@immutable
sealed class ChessState {}

final class ChessInitial extends ChessState {}

class ChessUpdatedState extends ChessState {
  final List<String> boardState;
  final int currentMoveIndex;
  ChessUpdatedState(this.boardState, this.currentMoveIndex);
}

class WonGame extends ChessState {}
class LostGame extends ChessState {}
class Draw extends ChessState {}
class StaleMate extends ChessState {}