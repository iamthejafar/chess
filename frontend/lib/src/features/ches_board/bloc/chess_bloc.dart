import 'package:bishop/bishop.dart';
import '../../../utils/chess_utils.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

part 'chess_event.dart';
part 'chess_state.dart';

class ChessBloc extends Bloc<ChessEvent, ChessState> {
  final Game game = Game(
    variant: Variant.standard(),
  );

  getTurnColor() {
    return ChessUtils.getTurnColor(game);
  }

  getPossibleMoves(int fromIndex) {
    return ChessUtils.getPossibleMoves(fromIndex, game);
  }

  ChessBloc() : super(ChessInitial()) {
    on<InitGame>((event, emit) {
      List<String> boardState = ChessUtils.fenToBoardState(game.fen);
      emit(ChessUpdatedState(boardState, 0));
    });

    on<MovePieceEvent>((event, emit) {
      final fromSquare = ChessUtils.indexToSquare(event.fromIndex);
      final toSquare = ChessUtils.indexToSquare(event.toIndex);
      if (game.makeMoveString(fromSquare + toSquare)) {
        List<String> boardState = ChessUtils.fenToBoardState(game.fen);
        int currentMoveIndex = game.history.length - 1;
        emit(ChessUpdatedState(boardState, currentMoveIndex));
      }
    });

    on<ForwardMove>((event, emit) {});
  }
}
