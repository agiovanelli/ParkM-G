import 'package:flutter/material.dart';
import '../models/operatore.dart';

class OperatorScreen extends StatelessWidget {
  final Operatore operatore;

  const OperatorScreen({
    super.key,
    required this.operatore,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Area Operatore'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Benvenuto, ${operatore.username}',
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              'Struttura: ${operatore.nomeStruttura}',
              style: const TextStyle(fontSize: 16),
            ),
            const SizedBox(height: 24),
            const Text(
              'Qui potrai aggiungere tutta la logica della schermata operatore\n'
              '(gestione parcheggi, posti, ecc.).',
            ),
          ],
        ),
      ),
    );
  }
}
