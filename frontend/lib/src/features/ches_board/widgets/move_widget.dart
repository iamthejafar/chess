import 'package:flutter/material.dart';

import '../models/move_model.dart';


class MoveWidget extends StatelessWidget {
  const MoveWidget({
    super.key,
    required this.move,
  });

  final MoveModel move;
  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        SizedBox(
            height: 20,
            width: 20,
            child: Image.asset(
                "assets/images/${move.pieceColor}${move.piece}.png")),
        Text(
          move.to ?? "",
          style: textTheme.bodyMedium!.copyWith(fontWeight: FontWeight.w600),
        ),
      ],
    );
  }
}
