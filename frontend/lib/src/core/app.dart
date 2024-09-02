import 'package:chess/src/features/ches_board/bloc/chess_bloc.dart';
import 'package:chess/src/features/landing/bloc/auth_bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';

import '../features/landing/screens/landing_screen.dart';
import 'colors.dart';


class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'Flutter Demo',
        theme: ThemeData(
          textTheme: GoogleFonts.latoTextTheme(),
          colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.black54,
          ),
          useMaterial3: true,
        ),
        home: MultiBlocProvider(
          child: const LandingScreen(),
          providers: [
            BlocProvider(create: (context) => AuthBloc()..add(InitAuth())),
            BlocProvider(create: (context) => ChessBloc())
          ],
        )
    );
  }
}
