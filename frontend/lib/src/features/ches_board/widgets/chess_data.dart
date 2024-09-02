
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../utils/chess_utils.dart';
import '../bloc/chess_bloc.dart';
import 'move_widget.dart';

class ChessData extends StatelessWidget {
  const ChessData({super.key});

  @override
  Widget build(BuildContext context) {
    int moveTableNumber = 0;
    final height = MediaQuery.of(context).size.height;
    final width = MediaQuery.of(context).size.width;
    final textTheme = Theme.of(context).textTheme;
    final chessBloc = context.read<ChessBloc>();
    return Expanded(
      child: Container(
        decoration: BoxDecoration(
            color: const Color(0xfff4858), borderRadius: BorderRadius.circular(8)),
        padding: const EdgeInsets.all(20),
        margin: const EdgeInsets.symmetric(horizontal: 50.0),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                Column(
                  children: [
                    const Icon(
                      Icons.timer_outlined,
                      color: Color(0xff2f4858),
                    ),
                    Text(
                      "Moves",
                      style: textTheme.bodyLarge!.copyWith(
                          color: const Color(0xff2f4858),
                          fontWeight: FontWeight.w700),
                    )
                  ],
                ),
                const Column(
                  children: [
                    Icon(CupertinoIcons.person_2_fill),
                    Text("Players")
                  ],
                )
              ],
            ),
            const Divider(),
            SizedBox(
              height: height * 0.5,
              child: chessBloc.game.history.length >= 2
                  ? GridView.builder(
                      shrinkWrap: true,
                      gridDelegate:
                          const SliverGridDelegateWithFixedCrossAxisCount(
                              crossAxisCount: 2,
                              childAspectRatio: 6,
                              mainAxisExtent: 30),
                      itemCount: chessBloc.game.history.length,
                      itemBuilder: (context, index) {
                        if (index + 1 == chessBloc.game.history.length) {
                          return const SizedBox();
                        }
                        final move = ChessUtils.extractMoveDetails(
                            chessBloc.game.history[index + 1]);
                        if (move != null) {
                          return Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 20),
                            child: Row(
                              children: [
                                if (index == 0)
                                  Padding(
                                    padding: const EdgeInsets.only(right: 30),
                                    child: Text(
                                      "1.",
                                      style: textTheme.bodyMedium!.copyWith(
                                          fontWeight: FontWeight.w600),
                                    ),
                                  )
                                else if (index % 2 == 0)
                                  Padding(
                                    padding: const EdgeInsets.only(right: 30),
                                    child: Text(
                                      "${index - moveTableNumber++}.",
                                      style: textTheme.bodyMedium!.copyWith(
                                          fontWeight: FontWeight.w600),
                                    ),
                                  ),
                                MoveWidget(
                                  move: move,
                                ),
                              ],
                            ),
                          );
                        } else {
                          return const SizedBox();
                        }
                      },
                    )
                  : const SizedBox(),
            ),
            const Divider(),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    IconButton(
                        onPressed: () {}, icon: const Icon(CupertinoIcons.back)),
                    IconButton(
                        onPressed: () {},
                        icon: const Icon(CupertinoIcons.arrow_clockwise)),
                    IconButton(
                        onPressed: () {}, icon: const Icon(CupertinoIcons.forward)),
                  ],
                ),
                IconButton(onPressed: () {}, icon: const Icon(Icons.share_outlined))
              ],
            ),
          ],
        ),
      ),
    );
  }
}
