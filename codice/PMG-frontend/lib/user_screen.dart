import 'package:flutter/material.dart';
import 'api_client.dart';
import 'utente.dart';

class UserScreen extends StatefulWidget {
  final Utente utente;
  final ApiClient apiClient;

  const UserScreen({
    super.key,
    required this.utente,
    required this.apiClient,
  });

  @override
  State<UserScreen> createState() => _UserScreenState();
}

class _UserScreenState extends State<UserScreen> {
  @override
  void initState() {
    super.initState();

    // Se non ci sono preferenze, mostra il dialog alla prima apertura
    if (widget.utente.preferenze == null ||
        widget.utente.preferenze!.isEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _showPreferenzeDialog();
      });
    }
  }

  void _showPreferenzeDialog() async {
    final updatedPrefs = await showDialog<Map<String, String>>(
      context: context,
      barrierDismissible: false,
      builder: (_) => PreferenzeDialog(
        utente: widget.utente,
        apiClient: widget.apiClient,
      ),
    );

    if (updatedPrefs != null) {
      setState(() {
        widget.utente.preferenze = updatedPrefs;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final prefs = widget.utente.preferenze;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Area Utente'),
        actions: [
          IconButton(
            icon: const Icon(Icons.tune),
            tooltip: 'Modifica preferenze',
            onPressed: _showPreferenzeDialog,
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Benvenuto, ${widget.utente.nome} ${widget.utente.cognome}',
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              'Email: ${widget.utente.email}',
              style: const TextStyle(fontSize: 16),
            ),
            const SizedBox(height: 24),
            const Text(
              'Le tue preferenze:',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            if (prefs == null || prefs.isEmpty)
              const Text('Nessuna preferenza impostata.')
            else
              ...prefs.entries.map(
                (e) => Text('${e.key}: ${e.value}'),
              ),
          ],
        ),
      ),
    );
  }
}

class PreferenzeDialog extends StatefulWidget {
  final Utente utente;
  final ApiClient apiClient;

  const PreferenzeDialog({
    super.key,
    required this.utente,
    required this.apiClient,
  });

  @override
  State<PreferenzeDialog> createState() => _PreferenzeDialogState();
}

class _PreferenzeDialogState extends State<PreferenzeDialog> {
  String _eta = 'under30';
  String _piano = 'piano_terra';
  double _distanza = 30;
  bool _disabile = false;
  bool _donnaIncinta = false;
  String _occupazione = 'Studente';

  bool _isSaving = false;

  final List<String> _occupazioni = const [
    'Studente',
    'Lavoratore dipendente',
    'Libero professionista',
  ];

  @override
  void initState() {
    super.initState();

    final prefs = widget.utente.preferenze;
    if (prefs != null && prefs.isNotEmpty) {
      _eta = prefs['età'] ?? _eta;
      _piano = prefs['piano'] ?? _piano;
      _distanza = double.tryParse(prefs['distanza'] ?? '') ?? _distanza;
      _disabile = (prefs['disabile'] ?? 'No') == 'Sì';
      _donnaIncinta = (prefs['donnaIncinta'] ?? 'No') == 'Sì';
      _occupazione = prefs['occupazione'] ?? _occupazione;
      if (!_occupazioni.contains(_occupazione)) {
        _occupazione = 'Studente';
      }
    }
  }

  Future<void> _onSalva() async {
    final prefs = <String, String>{
      'età': _eta,
      'piano': _piano,
      'distanza': _distanza.toStringAsFixed(2),
      'disabile': _disabile ? 'Sì' : 'No',
      'donnaIncinta': _donnaIncinta ? 'Sì' : 'No',
      'occupazione': _occupazione,
    };

    setState(() => _isSaving = true);
    try {
      await widget.apiClient.aggiornaPreferenze(widget.utente.id, prefs);
      if (!mounted) return;
      Navigator.of(context).pop(prefs);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            e is ApiException
                ? e.message
                : 'Errore nel salvataggio delle preferenze',
          ),
        ),
      );
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  void _onAnnulla() {
    Navigator.of(context).pop(); // nessun aggiornamento
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Imposta preferenze'),
      content: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Età'),
            Row(
              children: [
                Expanded(
                  child: RadioListTile<String>(
                    title: const Text('Under 30'),
                    value: 'under30',
                    groupValue: _eta,
                    onChanged: (v) => setState(() => _eta = v!),
                    dense: true,
                  ),
                ),
                Expanded(
                  child: RadioListTile<String>(
                    title: const Text('Over 60'),
                    value: 'over60',
                    groupValue: _eta,
                    onChanged: (v) => setState(() => _eta = v!),
                    dense: true,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Text('Piano'),
            Row(
              children: [
                Expanded(
                  child: RadioListTile<String>(
                    title: const Text('Piano terra'),
                    value: 'piano_terra',
                    groupValue: _piano,
                    onChanged: (v) => setState(() => _piano = v!),
                    dense: true,
                  ),
                ),
                Expanded(
                  child: RadioListTile<String>(
                    title: const Text('Altri piani'),
                    value: 'altri_piani',
                    groupValue: _piano,
                    onChanged: (v) => setState(() => _piano = v!),
                    dense: true,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Text('Distanza massima (m)'),
            Slider(
              value: _distanza,
              min: 0,
              max: 200,
              divisions: 40,
              label: _distanza.toStringAsFixed(0),
              onChanged: (v) => setState(() => _distanza = v),
            ),
            const SizedBox(height: 8),
            Text('Valore: ${_distanza.toStringAsFixed(0)} m'),
            const SizedBox(height: 12),
            const Text('Condizioni speciali'),
            CheckboxListTile(
              value: _disabile,
              onChanged: (v) => setState(() => _disabile = v ?? false),
              title: const Text('Disabile'),
              dense: true,
              controlAffinity: ListTileControlAffinity.leading,
            ),
            CheckboxListTile(
              value: _donnaIncinta,
              onChanged: (v) => setState(() => _donnaIncinta = v ?? false),
              title: const Text('Donna incinta'),
              dense: true,
              controlAffinity: ListTileControlAffinity.leading,
            ),
            const SizedBox(height: 12),
            const Text('Occupazione'),
            DropdownButton<String>(
              value: _occupazione,
              isExpanded: true,
              items: _occupazioni
                  .map(
                    (o) => DropdownMenuItem<String>(
                      value: o,
                      child: Text(o),
                    ),
                  )
                  .toList(),
              onChanged: (v) {
                if (v != null) {
                  setState(() => _occupazione = v);
                }
              },
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: _isSaving ? null : _onAnnulla,
          child: const Text('Annulla'),
        ),
        ElevatedButton(
          onPressed: _isSaving ? null : _onSalva,
          child: _isSaving
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Salva'),
        ),
      ],
    );
  }
}
