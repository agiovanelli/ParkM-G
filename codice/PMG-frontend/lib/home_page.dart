import 'package:flutter/material.dart';
import 'api_client.dart';
import 'user_screen.dart';
import 'operator_screen.dart';
import 'main.dart' show AppColors;

/// Pulsante primario con gradiente (equivalente a .primary-button)
class PrimaryButton extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;

  const PrimaryButton({
    super.key,
    required this.text,
    this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    final enabled = onPressed != null;
    return InkWell(
      borderRadius: BorderRadius.circular(20),
      onTap: enabled ? onPressed : null,
      child: Container(
        height: 44,
        decoration: BoxDecoration(
          gradient: enabled
              ? const LinearGradient(
                  begin: Alignment.centerLeft,
                  end: Alignment.centerRight,
                  colors: [
                    AppColors.accentBlue, // #3b82f6
                    AppColors.accentCyan, // #06b6d4
                  ],
                )
              : null,
          color: enabled ? null : Colors.grey.shade700,
          borderRadius: BorderRadius.circular(20),
          boxShadow: enabled
              ? [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.45),
                    blurRadius: 18,
                    offset: const Offset(0, 2),
                  ),
                ]
              : [],
        ),
        child: Center(
          child: Text(
            text,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
      ),
    );
  }
}

class HomePage extends StatefulWidget {
  final ApiClient apiClient;

  const HomePage({super.key, required this.apiClient});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  // stato toggle Accedi / Registrati
  bool _isLoginMode = true;

  // controller campi Clienti - login
  final _userLoginEmailController = TextEditingController();
  final _userLoginPasswordController = TextEditingController();

  // controller campi Clienti - registrazione
  final _userRegisterNameController = TextEditingController();
  final _userRegisterSurnameController = TextEditingController();
  final _userRegisterEmailController = TextEditingController();
  final _userRegisterPasswordController = TextEditingController();

  // controller campi Operatori
  final _operatorNomeStrutturaController = TextEditingController();
  final _operatorUsernameController = TextEditingController();

  bool _isLoading = false;

  @override
  void dispose() {
    _userLoginEmailController.dispose();
    _userLoginPasswordController.dispose();
    _userRegisterNameController.dispose();
    _userRegisterSurnameController.dispose();
    _userRegisterEmailController.dispose();
    _userRegisterPasswordController.dispose();
    _operatorNomeStrutturaController.dispose();
    _operatorUsernameController.dispose();
    super.dispose();
  }

  // ------------------ VALIDAZIONI ------------------

  bool _passwordValida(String pwd) {
    if (pwd.isEmpty) return false;
    final hasMinLen = pwd.length >= 6;
    final hasUpper = RegExp(r'[A-Z]').hasMatch(pwd);
    final hasDigit = RegExp(r'\d').hasMatch(pwd);
    final hasSpecial =
        RegExp(r'[!@#$%^&*()_+\-={}\[\]|:;"\<>,.?/]').hasMatch(pwd);
    return hasMinLen && hasUpper && hasDigit && hasSpecial;
  }

  void _showError(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg)),
    );
  }

  // ------------------ AZIONI CLIENTI ------------------

  Future<void> _handleUserLogin() async {
    final email = _userLoginEmailController.text.trim();
    final password = _userLoginPasswordController.text;

    if (email.isEmpty && password.isEmpty) {
      _showError('Inserisci email e password.');
      return;
    }
    if (email.isEmpty) {
      _showError('Inserisci email.');
      return;
    }
    if (password.isEmpty) {
      _showError('Inserisci password.');
      return;
    }
    if (!RegExp(r'^[A-Za-z0-9+_.-]+@(.+)$').hasMatch(email)) {
      _showError('Formato email non valido.');
      return;
    }

    setState(() => _isLoading = true);
    try {
      final utente = await widget.apiClient.loginUtente(email, password);

      if (!mounted) return;
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => UserScreen(
            utente: utente,
            apiClient: widget.apiClient,
          ),
        ),
      );
    } catch (e) {
      _showError(
          e is ApiException ? e.message : 'Errore di connessione al server utenti');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _handleUserRegister() async {
    final nome = _userRegisterNameController.text.trim();
    final cognome = _userRegisterSurnameController.text.trim();
    final email = _userRegisterEmailController.text.trim();
    final pwd = _userRegisterPasswordController.text;

    if (nome.isEmpty || cognome.isEmpty || email.isEmpty || pwd.isEmpty) {
      _showError('Compila tutti i campi.');
      return;
    }
    if (!RegExp(r'^[A-Za-z0-9+_.-]+@(.+)$').hasMatch(email)) {
      _showError('Email non valida.');
      return;
    }
    if (!_passwordValida(pwd)) {
      _showError(
        'La password deve contenere almeno:\n'
        '- 6 caratteri\n'
        '- 1 lettera maiuscola\n'
        '- 1 numero\n'
        '- 1 carattere speciale',
      );
      return;
    }

    setState(() => _isLoading = true);
    try {
      await widget.apiClient.registraUtente(nome, cognome, email, pwd);

      if (!mounted) return;
      setState(() {
        _isLoginMode = true;
      });
      _showError('Registrazione completata. Ora effettua il login.');
    } catch (e) {
      _showError(
          e is ApiException ? e.message : 'Errore di connessione al server utenti');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _handleUserForgotPassword() {
    _showError('Funzionalità "Password dimenticata?" non ancora disponibile.');
  }

  // ------------------ AZIONI OPERATORI ------------------

  Future<void> _handleOperatorLogin() async {
    final nomeStruttura = _operatorNomeStrutturaController.text.trim();
    final username = _operatorUsernameController.text.trim();

    if (nomeStruttura.isEmpty && username.isEmpty) {
      _showError('Inserisci il nome della struttura e l\'username.');
      return;
    }
    if (nomeStruttura.isEmpty) {
      _showError('Inserisci il nome della struttura.');
      return;
    }
    if (username.isEmpty) {
      _showError('Inserisci l\'username.');
      return;
    }

    setState(() => _isLoading = true);
    try {
      final operatore =
          await widget.apiClient.loginOperatore(nomeStruttura, username);

      if (!mounted) return;
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => OperatorScreen(operatore: operatore),
        ),
      );
    } catch (e) {
      _showError(
        e is ApiException ? e.message : 'Errore di connessione al server operatori',
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // ------------------ UI ------------------

  @override
  Widget build(BuildContext context) {
    final isWide = MediaQuery.of(context).size.width > 700;

    final branding = _buildBrandingPanel();
    final tabs = _buildTabs();

    return Scaffold(
      body: Container(
        // .root: gradient di sfondo
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              AppColors.bgDark,  // #020617
              AppColors.bgDark,  // 40%
              AppColors.bgDark2, // #0b1120
            ],
          ),
        ),
        child: Stack(
          children: [
            SafeArea(
              child: isWide
                  ? Row(
                      children: [
                        SizedBox(width: 280, child: branding),
                        const VerticalDivider(width: 1, color: Colors.transparent),
                        Expanded(child: tabs),
                      ],
                    )
                  : Column(
                      children: [
                        SizedBox(height: 220, child: branding),
                        const Divider(height: 1, color: Colors.transparent),
                        Expanded(child: tabs),
                      ],
                    ),
            ),
            if (_isLoading)
              Container(
                color: Colors.black45,
                child: const Center(child: CircularProgressIndicator()),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildBrandingPanel() {
    // .branding-pane
    return Container(
      decoration: const BoxDecoration(
        borderRadius: BorderRadius.only(
          topRight: Radius.circular(32),
          bottomRight: Radius.circular(32),
        ),
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            AppColors.brandTop, // #0f172a
            AppColors.bgDark,   // #020617
          ],
        ),
        boxShadow: [
          BoxShadow(
            color: Color.fromRGBO(0, 0, 0, 0.4),
            blurRadius: 25,
            spreadRadius: 0,
            offset: Offset(0, 0),
          ),
        ],
      ),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 260),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: const [
              Text(
                'Park M&G',
                style: TextStyle(
                  color: AppColors.textPrimary,
                  fontSize: 32,
                  fontWeight: FontWeight.bold,
                ),
              ),
              SizedBox(height: 16),
              Text(
                'Gestione utenti e operatori',
                style: TextStyle(
                  color: AppColors.textSecondary, // #e5e7eb
                  fontSize: 14,
                ),
                textAlign: TextAlign.center,
              ),
              SizedBox(height: 16),
              Text(
                'Accedi o registra un nuovo account utente',
                style: TextStyle(
                  color: AppColors.textMuted, // #9ca3af
                  fontSize: 12,
                ),
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTabs() {
    // TabPane + header area tipo segmented-control
    return DefaultTabController(
      length: 2,
      child: Column(
        children: [
          const SizedBox(height: 24),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 40),
            child: Container(
              padding: const EdgeInsets.all(4),
              decoration: BoxDecoration(
                color: AppColors.bgDark, // #020617
                borderRadius: BorderRadius.circular(999),
              ),
              child: TabBar(
                indicator: BoxDecoration(
                  color: const Color(0xFF111827), // #111827
                  borderRadius: BorderRadius.circular(999),
                ),
                indicatorSize: TabBarIndicatorSize.tab,
                labelColor: AppColors.textSecondary, // #e5e7eb
                unselectedLabelColor: AppColors.textMuted, // #9ca3af
                dividerColor: Colors.transparent,
                tabs: const [
                  Tab(text: 'Clienti'),
                  Tab(text: 'Operatori'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          Expanded(
            child: TabBarView(
              children: [
                _buildClientiTab(),
                _buildOperatoriTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildClientiTab() {
    // .card centrale
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 420),
          child: Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: AppColors.bgDark, // #020617
              borderRadius: BorderRadius.circular(24),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.55),
                  blurRadius: 26,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // “segmented-control” Accedi / Registrati
                Container(
                  padding: const EdgeInsets.all(4),
                  decoration: BoxDecoration(
                    color: AppColors.bgDark,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: _buildSegmentButton(
                          text: 'Accedi',
                          selected: _isLoginMode,
                          onTap: () {
                            setState(() => _isLoginMode = true);
                          },
                          isLeft: true,
                        ),
                      ),
                      const SizedBox(width: 4),
                      Expanded(
                        child: _buildSegmentButton(
                          text: 'Registrati',
                          selected: !_isLoginMode,
                          onTap: () {
                            setState(() => _isLoginMode = false);
                          },
                          isLeft: false,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 24),
                if (_isLoginMode) _buildLoginForm() else _buildRegisterForm(),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSegmentButton({
    required String text,
    required bool selected,
    required VoidCallback onTap,
    required bool isLeft,
  }) {
    final radius = isLeft
        ? const BorderRadius.horizontal(
            left: Radius.circular(999), right: Radius.circular(24))
        : const BorderRadius.horizontal(
            left: Radius.circular(24), right: Radius.circular(999));

    return InkWell(
      borderRadius: radius,
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFF111827) : Colors.transparent,
          borderRadius: radius,
        ),
        alignment: Alignment.center,
        child: Text(
          text,
          style: TextStyle(
            color:
                selected ? AppColors.textSecondary : AppColors.textMuted,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }

  Widget _buildLoginForm() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Accedi al tuo account',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: _userLoginEmailController,
          decoration: const InputDecoration(
            labelText: 'Email',
          ),
          keyboardType: TextInputType.emailAddress,
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _userLoginPasswordController,
          decoration: const InputDecoration(
            labelText: 'Password',
          ),
          obscureText: true,
        ),
        const SizedBox(height: 16),
        PrimaryButton(
          text: 'Accedi',
          onPressed: _handleUserLogin,
        ),
        TextButton(
          onPressed: _handleUserForgotPassword,
          style: TextButton.styleFrom(
            foregroundColor: const Color(0xFF60A5FA), // hyperlink
          ),
          child: const Text(
            'Password dimenticata?',
            style: TextStyle(fontSize: 12),
          ),
        ),
      ],
    );
  }

  Widget _buildRegisterForm() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Crea un nuovo account',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: _userRegisterNameController,
          decoration: const InputDecoration(
            labelText: 'Nome',
          ),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _userRegisterSurnameController,
          decoration: const InputDecoration(
            labelText: 'Cognome',
          ),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _userRegisterEmailController,
          decoration: const InputDecoration(
            labelText: 'Email',
          ),
          keyboardType: TextInputType.emailAddress,
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _userRegisterPasswordController,
          decoration: const InputDecoration(
            labelText: 'Password',
          ),
          obscureText: true,
        ),
        const SizedBox(height: 16),
        PrimaryButton(
          text: 'Registrati',
          onPressed: _handleUserRegister,
        ),
      ],
    );
  }

  Widget _buildOperatoriTab() {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 420),
          child: Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: AppColors.bgDark,
              borderRadius: BorderRadius.circular(24),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.55),
                  blurRadius: 26,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Login Operatore',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: _operatorNomeStrutturaController,
                  decoration: const InputDecoration(
                    labelText: 'Nome Struttura',
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _operatorUsernameController,
                  decoration: const InputDecoration(
                    labelText: 'Username',
                  ),
                  obscureText: true, // come PasswordField in JavaFX
                ),
                const SizedBox(height: 16),
                PrimaryButton(
                  text: 'Accedi',
                  onPressed: _handleOperatorLogin,
                ),
                const SizedBox(height: 8),
                const Text(
                  'Accesso riservato al personale autorizzato.',
                  style: TextStyle(
                    fontSize: 11,
                    color: AppColors.textMuted,
                  ),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
