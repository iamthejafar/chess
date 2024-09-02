import 'package:chess/src/features/ches_board/widgets/chess_data.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../bloc/chess_bloc.dart';
import '../widgets/chess_board.dart';

class ChessScreen extends StatelessWidget {
  const ChessScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: BlocProvider(
        create: (context)=>ChessBloc()..add(InitGame()),
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: BlocConsumer<ChessBloc, ChessState>(
            listener: (context,state){},
            builder: (context, state) {
              final chessBloc = BlocProvider.of<ChessBloc>(context);
              if (state is ChessUpdatedState) {
                return Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    ChessBoard(
                      boardState: state.boardState,
                      onMovePiece: (from, to) {
                        context.read<ChessBloc>().add(MovePieceEvent(from, to));
                      },
                      turnColor: chessBloc.getTurnColor(),
                      getPossibleMoves:
                          (fromIndex) => chessBloc.getPossibleMoves(fromIndex),
                    ),
                    const ChessData(),
                  ],
                );
              }
              return const CircularProgressIndicator();
            },
          ),
        ),
      )
    );
  }
}






