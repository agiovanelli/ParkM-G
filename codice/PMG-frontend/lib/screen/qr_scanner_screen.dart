import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:park_mg/utils/theme.dart';

class QrScannerPage extends StatefulWidget {
  final Function(String) onQrScanned;

  const QrScannerPage({super.key, required this.onQrScanned});

  @override
  State<QrScannerPage> createState() => _QrScannerPageState();
}

class _QrScannerPageState extends State<QrScannerPage> {
  MobileScannerController cameraController = MobileScannerController();
  bool _isProcessing = false;
  String? _lastScannedCode;
  String? _errorMessage;
  String? _successMessage;

  @override
  void dispose() {
    cameraController.dispose();
    super.dispose();
  }

  void _onDetect(BarcodeCapture capture) async {
    if (_isProcessing) return;

    final List<Barcode> barcodes = capture.barcodes;
    if (barcodes.isEmpty) return;

    final String? code = barcodes.first.rawValue;
    if (code == null || code.isEmpty) return;

    // Evita di processare lo stesso codice più volte
    if (_lastScannedCode == code) return;

    setState(() {
      _isProcessing = true;
      _lastScannedCode = code;
      _errorMessage = null;
      _successMessage = null;
    });

    try {
      // Chiama la funzione passata dal parent (che farà la chiamata API)
      await widget.onQrScanned(code);
      
      if (mounted) {
        setState(() {
          _successMessage = 'Accesso validato con successo!';
          _isProcessing = false;
        });

        // Resetta dopo 3 secondi per permettere una nuova scansione
        await Future.delayed(const Duration(seconds: 3));
        if (mounted) {
          setState(() {
            _lastScannedCode = null;
            _successMessage = null;
          });
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString().replaceAll('Exception: ', '');
          _isProcessing = false;
        });

        // Resetta dopo 4 secondi
        await Future.delayed(const Duration(seconds: 4));
        if (mounted) {
          setState(() {
            _lastScannedCode = null;
            _errorMessage = null;
          });
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.bgDark,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderField, width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.35),
            blurRadius: 22,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.accentCyan.withOpacity(0.12),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(
                    color: AppColors.accentCyan.withOpacity(0.35),
                    width: 1,
                  ),
                ),
                child: const Icon(
                  Icons.qr_code_scanner,
                  color: AppColors.accentCyan,
                  size: 24,
                ),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Text(
                  'Scansione QR',
                  style: TextStyle(
                    color: AppColors.textPrimary,
                    fontSize: 18,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
            ],
          ),

          const SizedBox(height: 16),

          // Istruzioni
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.bgDark2.withOpacity(0.25),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.borderField, width: 1),
            ),
            child: Row(
              children: [
                Icon(
                  Icons.info_outline,
                  color: AppColors.accentCyan.withOpacity(0.8),
                  size: 20,
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Inquadra il codice QR del veicolo per validare l\'ingresso',
                    style: TextStyle(
                      color: AppColors.textSecondary,
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                    ),
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 16),

          // Scanner Area
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(16),
              child: Stack(
                children: [
                  MobileScanner(
                    controller: cameraController,
                    onDetect: _onDetect,
                  ),
                  
                  // Overlay con frame di scansione
                  CustomPaint(
                    painter: _ScannerOverlayPainter(),
                    child: Container(),
                  ),

                  // Status overlay
                  if (_isProcessing || _errorMessage != null || _successMessage != null)
                    Container(
                      color: Colors.black.withOpacity(0.7),
                      child: Center(
                        child: Container(
                          margin: const EdgeInsets.all(24),
                          padding: const EdgeInsets.all(20),
                          decoration: BoxDecoration(
                            color: AppColors.bgDark,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(
                              color: AppColors.borderField,
                              width: 1,
                            ),
                          ),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              if (_isProcessing)
                                const CircularProgressIndicator(
                                  color: AppColors.accentCyan,
                                ),
                              if (_errorMessage != null)
                                Icon(
                                  Icons.error_outline,
                                  color: const Color(0xFFEF4444),
                                  size: 48,
                                ),
                              if (_successMessage != null)
                                Icon(
                                  Icons.check_circle_outline,
                                  color: const Color(0xFF10B981),
                                  size: 48,
                                ),
                              const SizedBox(height: 16),
                              Text(
                                _isProcessing
                                    ? 'Validazione in corso...'
                                    : _errorMessage ?? _successMessage ?? '',
                                textAlign: TextAlign.center,
                                style: TextStyle(
                                  color: _errorMessage != null
                                      ? const Color(0xFFEF4444)
                                      : _successMessage != null
                                          ? const Color(0xFF10B981)
                                          : AppColors.textPrimary,
                                  fontSize: 16,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // Controlli camera
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _controlButton(
              icon: Icons.flash_off,
              activeIcon: Icons.flash_on,
              label: 'Flash',
              onTap: () => cameraController.toggleTorch(),
            ),
            const SizedBox(width: 8), // Ridotto un po' il margine per far stare 3 bottoni
            _controlButton(
              icon: Icons.edit_note, // Icona per inserimento manuale
              label: 'Manuale',
              onTap: _showManualEntryDialog,
            ),
            const SizedBox(width: 8),
            _controlButton(
              icon: Icons.cameraswitch,
              label: 'Cambia',
              onTap: () => cameraController.switchCamera(),
            ),
          ],
        ),
        ],
      ),
    );
  }

  Widget _controlButton({
    required IconData icon,
    IconData? activeIcon,
    required String label,
    required VoidCallback onTap,
  }) {
    return InkWell(
      borderRadius: BorderRadius.circular(14),
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        decoration: BoxDecoration(
          color: AppColors.bgDark2.withOpacity(0.35),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: AppColors.borderField, width: 1),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: AppColors.textPrimary, size: 20),
            const SizedBox(width: 8),
            Text(
              label,
              style: const TextStyle(
                color: AppColors.textPrimary,
                fontWeight: FontWeight.w700,
                fontSize: 14,
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showManualEntryDialog() {
  final TextEditingController manualController = TextEditingController();

  showDialog(
    context: context,
    builder: (context) => AlertDialog(
      backgroundColor: AppColors.bgDark,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: const BorderSide(color: AppColors.borderField),
      ),
      title: const Text(
        'Inserimento Manuale',
        style: TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.bold),
      ),
      content: TextField(
        controller: manualController,
        style: const TextStyle(color: AppColors.textPrimary),
        decoration: InputDecoration(
          hintText: 'Inserisci il codice...',
          hintStyle: TextStyle(color: AppColors.textSecondary.withOpacity(0.5)),
          enabledBorder: OutlineInputBorder(
            borderSide: const BorderSide(color: AppColors.borderField),
            borderRadius: BorderRadius.circular(12),
          ),
          focusedBorder: OutlineInputBorder(
            borderSide: const BorderSide(color: AppColors.accentCyan),
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ), // <--- Questa parentesi chiude il TextField correttamente
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Annulla', style: TextStyle(color: AppColors.textSecondary)),
        ),
        ElevatedButton(
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.accentCyan,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          ),
          onPressed: () {
            final code = manualController.text.trim();
            if (code.isNotEmpty) {
              Navigator.pop(context);
              _onDetect(BarcodeCapture(barcodes: [Barcode(rawValue: code)]));
            }
          },
          child: const Text(
            'Conferma', 
            style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold)
          ),
        ),
      ],
    ),
  );
}
}

// Custom painter per il frame di scansione
class _ScannerOverlayPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.black.withOpacity(0.5)
      ..style = PaintingStyle.fill;

    final framePaint = Paint()
      ..color = AppColors.accentCyan
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3;

    final cornerPaint = Paint()
      ..color = AppColors.accentCyan
      ..style = PaintingStyle.stroke
      ..strokeWidth = 5
      ..strokeCap = StrokeCap.round;

    // Dimensioni del frame centrale
    final frameSize = size.width * 0.7;
    final left = (size.width - frameSize) / 2;
    final top = (size.height - frameSize) / 2;
    final rect = Rect.fromLTWH(left, top, frameSize, frameSize);

    // Disegna l'overlay scuro con il buco centrale
    final path = Path()
      ..addRect(Rect.fromLTWH(0, 0, size.width, size.height))
      ..addRRect(RRect.fromRectAndRadius(rect, const Radius.circular(16)))
      ..fillType = PathFillType.evenOdd;

    canvas.drawPath(path, paint);

    // Disegna il frame
    canvas.drawRRect(
      RRect.fromRectAndRadius(rect, const Radius.circular(16)),
      framePaint,
    );

    // Disegna gli angoli
    const cornerLength = 30.0;
    
    // Top-left
    canvas.drawLine(
      Offset(left, top + 16),
      Offset(left, top + cornerLength),
      cornerPaint,
    );
    canvas.drawLine(
      Offset(left + 16, top),
      Offset(left + cornerLength, top),
      cornerPaint,
    );

    // Top-right
    canvas.drawLine(
      Offset(left + frameSize - 16, top),
      Offset(left + frameSize - cornerLength, top),
      cornerPaint,
    );
    canvas.drawLine(
      Offset(left + frameSize, top + 16),
      Offset(left + frameSize, top + cornerLength),
      cornerPaint,
    );

    // Bottom-left
    canvas.drawLine(
      Offset(left, top + frameSize - 16),
      Offset(left, top + frameSize - cornerLength),
      cornerPaint,
    );
    canvas.drawLine(
      Offset(left + 16, top + frameSize),
      Offset(left + cornerLength, top + frameSize),
      cornerPaint,
    );

    // Bottom-right
    canvas.drawLine(
      Offset(left + frameSize - 16, top + frameSize),
      Offset(left + frameSize - cornerLength, top + frameSize),
      cornerPaint,
    );
    canvas.drawLine(
      Offset(left + frameSize, top + frameSize - 16),
      Offset(left + frameSize, top + frameSize - cornerLength),
      cornerPaint,
    );
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}