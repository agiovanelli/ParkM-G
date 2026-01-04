import 'package:flutter/material.dart';
import '../api/api_client.dart';
import '../models/prenotazione.dart';
import '../models/utente.dart';
import '../utils/theme.dart';
import '../widgets/prenotazione_dialog.dart';


class HistoryScreen extends StatefulWidget {
  final Utente utente;
  final ApiClient apiClient;

  const HistoryScreen({super.key, required this.utente, required this.apiClient});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  late Future<List<PrenotazioneResponse>> _storicoFuture;

  @override
  void initState() {
    super.initState();
    _storicoFuture = widget.apiClient.getStoricoPrenotazioni(widget.utente.id);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgDark,
      appBar: AppBar(
        title: const Text("Le mie Prenotazioni", style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: AppColors.bgDark2,
        elevation: 0,
      ),
      body: FutureBuilder<List<PrenotazioneResponse>>(
        future: _storicoFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator(color: AppColors.accentCyan));
          } else if (snapshot.hasError) {
            return Center(child: Text("Errore: ${snapshot.error}", style: const TextStyle(color: Colors.redAccent)));
          } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
            return const Center(
              child: Text("Non hai ancora effettuato prenotazioni.", style: TextStyle(color: AppColors.textMuted))
            );
          }

          final storico = snapshot.data!;
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: storico.length,
            itemBuilder: (context, index) {
              final p = storico[index];
              return Card(
                color: AppColors.bgDark2,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                margin: const EdgeInsets.only(bottom: 12),
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                  leading: const Icon(Icons.directions_car, color: AppColors.accentCyan),
                  title: Text("Parcheggio: ${p.parcheggioId}", style: const TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.bold)),
                  subtitle: Text("Data: ${p.orario.substring(8, 10)}/${p.orario.substring(5, 7)}/${p.orario.substring(0, 4)} ore ${p.orario.substring(11, 16)}",
                  style: const TextStyle(color: AppColors.textMuted),
    ),
                  trailing: const Icon(Icons.chevron_right, color: AppColors.textMuted),
                  onTap: () {
                    PrenotazioneDialog.mostra(context, p);
                  },
                ),
              );
            },
          );
        },
      ),
    );
  }
}