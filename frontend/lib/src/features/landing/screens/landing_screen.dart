import 'package:chess/src/features/landing/bloc/auth_bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:gap/gap.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:google_sign_in_web/web_only.dart' as web;

import '../../ches_board/screens/chess_board_screen.dart';

class LandingScreen extends StatelessWidget {
  const LandingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.of(context).size.width;
    final textTheme = Theme.of(context).textTheme;
    final authBloc = context.read<AuthBloc>();
    return Scaffold(
      body: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 50.0),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [

            SizedBox(
              width: width * 0.4,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text("Chess",style: textTheme.displayMedium!.copyWith(fontWeight: FontWeight.w700),),
                  Gap(10),
                  Text(
                    "Discover the thrill of strategic gameplay on our new chess website, where classic chess meets modern design. Play, learn, and challenge yourself with a seamless online experience tailored for enthusiasts of all levels.",
                    style: textTheme.bodyLarge,
                  ),
                  Gap(10),
                  Row(
                    children: [
                      InkWell(
                        onTap: () {
                          Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (context)=>ChessScreen()));
                        },
                        child: Container(
                          padding:  EdgeInsets.symmetric(vertical: 5, horizontal: 15,),
                          decoration:  BoxDecoration(
                              border: Border.all(color:Color(0xffbfa5a5)),
                              borderRadius: BorderRadius.circular(8)
                          ),
                          child: Text("Play Now",style: textTheme.bodyLarge!.copyWith(fontWeight: FontWeight.w500),),),
                      ),
                      Gap(10),


                      web.renderButton(
                        configuration: web.GSIButtonConfiguration(

                        )
                      )
                      // InkWell(
                      //   onTap: () async {
                      //
                      //     authBloc.add(GoogleSignInRequested());
                      //     // const List<String> scopes = <String>[
                      //     //   'email',
                      //     //   'https://www.googleapis.com/auth/contacts.readonly',
                      //     // ];
                      //     // GoogleSignIn googleSignIn = GoogleSignIn(
                      //     //   clientId: "",
                      //     //   scopes: scopes
                      //     // );
                      //     //
                      //     // try{
                      //     //   await googleSignIn.signIn();
                      //     // } catch(e){
                      //     //   print(e);
                      //     // }
                      //   },
                      //   child: Container(
                      //      padding:  EdgeInsets.symmetric(vertical: 5, horizontal: 15,),
                      //     decoration:  BoxDecoration(
                      //       color: Color(0xffbfa5a5),
                      //       borderRadius: BorderRadius.circular(8)
                      //     ),
                      //     child: Text("Sign In",style: textTheme.bodyLarge!.copyWith(fontWeight: FontWeight.w500),),),
                      // ),
                    ],
                  )
                ],
              ),
            ),

            Container(
              width: width*0.3,
              height: width*0.3,
              decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(10),
                  boxShadow: [
                  BoxShadow(color: Colors.black54.withOpacity(0.1),blurRadius: 4)
                ]
              ),
              child: ClipRRect(
                          borderRadius: BorderRadius.circular(10),
              child: Image.asset("assets/images/board.png", fit: BoxFit.fitWidth,)),
            )
          ],
        ),
      ),
    );
  }
}



Widget buildSignInButton({Function? onPressed}) {
  return web.renderButton();
}


