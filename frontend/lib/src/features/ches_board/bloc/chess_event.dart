
part of 'chess_bloc.dart';

@immutable
sealed class ChessEvent {}


class MovePieceEvent extends ChessEvent {
  final int fromIndex;
  final int toIndex;

  MovePieceEvent(this.fromIndex, this.toIndex);
}

class InitGame extends ChessEvent {}

class ForwardMove extends ChessEvent {}

class BackwardMove extends ChessEvent {}


class WonGameEvent extends ChessEvent {}

class LossGameEvent extends ChessEvent {}

class DrawGameEvent extends ChessEvent {}
