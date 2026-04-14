import 'package:flutter/material.dart';

import 'package:park_mg/utils/theme.dart';
import '../../api/api_client.dart';
import '../../models/utente.dart';

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
  double _distanza = 1;
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
      _eta = prefs['eta'] ?? _eta;
      _piano = prefs['piano'] ?? _piano;

      final distanzaRaw = prefs['distanza'];
      final distanzaParsed =
          double.tryParse(distanzaRaw?.toString() ?? '1') ?? 1;
      _distanza = distanzaParsed.clamp(1.0, 4.0);

      _disabile = (prefs['disabile'] ?? 'No') == 'Si';
      _donnaIncinta = (prefs['donnaIncinta'] ?? 'No') == 'Si';
      _occupazione = prefs['occupazione'] ?? _occupazione;
      if (!_occupazioni.contains(_occupazione)) _occupazione = 'Studente';
    }
  }

  Future<void> _onSalva() async {
    final prefs = <String, String>{
      'eta': _eta,
      'piano': _piano,
      'distanza': _distanza.toStringAsFixed(0),
      'disabile': _disabile ? 'Si' : 'No',
      'donnaIncinta': _donnaIncinta ? 'Si' : 'No',
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
          behavior: SnackBarBehavior.floating,
          backgroundColor: AppColors.bgDark,
          content: Text(
            e is ApiException
                ? e.message
                : 'Errore nel salvataggio delle preferenze',
            style: const TextStyle(color: AppColors.textPrimary),
          ),
        ),
      );
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  void _onAnnulla() => Navigator.of(context).pop();

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppColors.bgDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      title: const Text(
        'Imposta preferenze',
        style: TextStyle(
          color: AppColors.textPrimary,
          fontWeight: FontWeight.w800,
        ),
      ),
      content: SingleChildScrollView(
        child: Theme(
          data: Theme.of(context).copyWith(
            radioTheme: RadioThemeData(
              fillColor: WidgetStateProperty.all(AppColors.accentCyan),
            ),
            checkboxTheme: CheckboxThemeData(
              fillColor: WidgetStateProperty.resolveWith((states) {
                if (states.contains(WidgetState.selected)) return AppColors.accentCyan;
                return AppColors.borderField;
              }),
              checkColor: WidgetStateProperty.all(AppColors.textPrimary),
            ),
            sliderTheme: Theme.of(context).sliderTheme.copyWith(
              activeTrackColor: AppColors.accentCyan,
              thumbColor: AppColors.accentCyan,
              overlayColor: AppColors.accentCyan.withValues(alpha: 0.15),
              inactiveTrackColor: AppColors.borderField,
              valueIndicatorColor: AppColors.brandTop,
              valueIndicatorTextStyle: const TextStyle(
                color: AppColors.textPrimary,
              ),
            ),
          ),
          child: DefaultTextStyle(
            style: const TextStyle(color: AppColors.textPrimary),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Età',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                Row(
                  children: [
                    Expanded(
                      child: RadioListTile<String>(
                        title: const Text('Under 30'),
                        value: 'under30',
                        groupValue: _eta,
                        onChanged: (v) => setState(() => _eta = v!),
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                    Expanded(
                      child: RadioListTile<String>(
                        title: const Text('Over 60'),
                        value: 'over60',
                        groupValue: _eta,
                        onChanged: (v) => setState(() => _eta = v!),
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),

                const Text(
                  'Piano',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                Row(
                  children: [
                    Expanded(
                      child: RadioListTile<String>(
                        title: const Text('Piano terra'),
                        value: 'piano_terra',
                        groupValue: _piano,
                        onChanged: (v) => setState(() => _piano = v!),
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                    Expanded(
                      child: RadioListTile<String>(
                        title: const Text('Altri piani'),
                        value: 'altri_piani',
                        groupValue: _piano,
                        onChanged: (v) => setState(() => _piano = v!),
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),

                const Text(
                  'Distanza massima (1: vicino - 4: lontano)',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                Slider(
                  value: _distanza.clamp(1.0, 4.0),
                  min: 1,
                  max: 4,
                  divisions: 3,
                  label: _distanza.toStringAsFixed(0),
                  onChanged: (v) => setState(() => _distanza = v),
                ),
                Text(
                  'Valore: ${_distanza.toStringAsFixed(0)}',
                  style: TextStyle(
                    color: AppColors.textMuted.withValues(alpha: 0.95),
                  ),
                ),
                const SizedBox(height: 10),

                const Text(
                  'Condizioni speciali',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                CheckboxListTile(
                  value: _disabile,
                  onChanged: (v) => setState(() => _disabile = v ?? false),
                  title: const Text('Disabile'),
                  dense: true,
                  controlAffinity: ListTileControlAffinity.leading,
                  contentPadding: EdgeInsets.zero,
                ),
                CheckboxListTile(
                  value: _donnaIncinta,
                  onChanged: (v) => setState(() => _donnaIncinta = v ?? false),
                  title: const Text('Donna incinta'),
                  dense: true,
                  controlAffinity: ListTileControlAffinity.leading,
                  contentPadding: EdgeInsets.zero,
                ),
                const SizedBox(height: 10),

                const Text(
                  'Occupazione',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                DropdownButtonFormField<String>(
                  initialValue: _occupazione,
                  dropdownColor: AppColors.bgDark,
                  decoration: InputDecoration(
                    filled: true,
                    fillColor: AppColors.bgDark2.withValues(alpha: 0.35),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(
                        color: AppColors.borderField,
                      ),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: AppColors.accentCyan),
                    ),
                  ),
                  items: _occupazioni
                      .map(
                        (o) =>
                            DropdownMenuItem<String>(value: o, child: Text(o)),
                      )
                      .toList(),
                  onChanged: (v) {
                    if (v != null) setState(() => _occupazione = v);
                  },
                ),
              ],
            ),
          ),
        ),
      ),
      actionsPadding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
      actions: [
        TextButton(
          onPressed: _isSaving ? null : _onAnnulla,
          style: TextButton.styleFrom(foregroundColor: AppColors.textMuted),
          child: const Text('Annulla'),
        ),
        ElevatedButton(
          onPressed: _isSaving ? null : _onSalva,
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.accentCyan,
            foregroundColor: AppColors.textPrimary,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
          child: _isSaving
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: AppColors.textPrimary,
                  ),
                )
              : const Text(
                  'Salva',
                  style: TextStyle(fontWeight: FontWeight.w800),
                ),
        ),
      ],
    );
  }
}
