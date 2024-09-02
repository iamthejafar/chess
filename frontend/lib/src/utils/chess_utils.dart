import 'package:bishop/bishop.dart';

import '../features/ches_board/models/move_model.dart';

class ChessUtils {
  static String indexToSquare(int index) {
    final rowIndex = index ~/ 8;
    final colIndex = index % 8;
    final row = 8 - rowIndex;
    final col = String.fromCharCode('a'.codeUnitAt(0) + colIndex);
    return '$col$row';
  }

  static int squareToIndex(String square) {
    final col = square.codeUnitAt(0) - 'a'.codeUnitAt(0);
    final row = 8 - int.parse(square[1]);
    return row * 8 + col;
  }

  static List<String> fenToBoardState(String fen) {
    List<String> boardState = [];
    List<String> ranks = fen.split('/');
    for (var rank in ranks) {
      for (var char in rank.split('')) {
        if (RegExp(r'\d').hasMatch(char)) {
          int emptySquares = int.parse(char);
          boardState.addAll(List.filled(emptySquares, '.'));
        } else {
          boardState.add(char);
        }
      }
    }
    return boardState;
  }


  static String convertBoardStringToFEN(String board) {
    List<String> fenRows = [];
    List<String> rows = board.trim().split('\n');

    for (String row in rows) {
      row = row.trim();
      if (row.contains('+') || row.contains('a')) {
        continue;
      }
      row = row.replaceAll(RegExp(r'[0-9]|[\|\s]'), '');

      String fenRow = '';
      int emptyCount = 0;

      for (int i = 0; i < row.length; i++) {
        String square = row[i];
        if (square == '.') {
          emptyCount++;
        } else {
          if (emptyCount > 0) {
            fenRow += emptyCount.toString();
            emptyCount = 0;
          }
          fenRow += square;
        }
      }

      if (emptyCount > 0) {
        fenRow += emptyCount.toString();
      }

      fenRows.add(fenRow);
    }

    return '${fenRows.join('/')} w KQkq - 0 1';
  }

  static MoveModel? extractMoveDetails(BishopState state) {
    if (state.move == null) {
      return null;
    }

    Move move = state.move!;
    String fromSquare = state.meta?.variant.boardSize.squareName(move.from) ?? '';
    String toSquare = state.meta?.variant.boardSize.squareName(move.to) ?? '';

    String pieceMoved = state.pieceOnSquare(toSquare);

    return MoveModel(
        piece: pieceMoved,
        from: fromSquare,
        to: toSquare,
        pieceColor: state.turn == 0 ? "b" : "w");
  }


  static List<int> getPossibleMoves(int fromIndex, Game game) {
    String fromSquare = indexToSquare(fromIndex);

    List<Move> moves = game.generateLegalMoves();

    List<int> possibleMoves = [];

    for (var move in moves) {
      String squareName = game.variant.boardSize.squareName(move.from);
      String toName = game.variant.boardSize.squareName(move.to);
      if (squareName == fromSquare) {
        possibleMoves.add(squareToIndex(toName));
      }
    }

    return possibleMoves;
  }

  static String getTurnColor(Game game) {
    return game.turn == 1 ? "white" : "black";
  }

}
