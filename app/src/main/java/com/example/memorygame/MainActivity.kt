package com.example.memorygame

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator


// IMPORTS NECESSÁRIOS PARA O KONFETTI
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size



class MainActivity : AppCompatActivity() {

    // --- Variáveis de UI ---
    private lateinit var gameTitle: TextView
    private lateinit var scoreText: TextView
    private lateinit var timerText: TextView
    private lateinit var restartGameButton: Button // Botão "Começar Jogo" da tela inicial
    private lateinit var gameEndRestartButton: Button // Botão "Reiniciar Jogo" do fim do jogo
    private lateinit var cardImages: List<ImageView>

    // Variáveis para os novos elementos de UI
    private lateinit var gameRulesText: TextView // O manual/regras
    private lateinit var gameStatusLayout: LinearLayout // O LinearLayout que contém scoreText e timerText
    private lateinit var cardGridLayout: GridLayout     // O GridLayout que contém as cartas
    private lateinit var inGameAdminButtonsLayout: LinearLayout // Layout para botões de admin em jogo

    // VARIÁVEL PARA O KONFETTI
    private lateinit var konfettiView: KonfettiView


    // --- Variáveis do Placar ---
    private lateinit var highScoreTitle: TextView
    private lateinit var highScore1: TextView
    private lateinit var highScore2: TextView
    private lateinit var highScore3: TextView
    private lateinit var highScore4: TextView
    private lateinit var highScore5: TextView
    private lateinit var resetScoresButton: Button
    private lateinit var addMinuteButton: Button
    private lateinit var togglePeekButton: Button // Botão para ativar/desativar "revelar cartas" no início
    private lateinit var revealCardsInGameButton: Button // Botão para revelar cartas durante o jogo
    private lateinit var goToPhase3Button: Button // NOVO: Botão para ir para a Fase 3
    private lateinit var pointsChangeText: TextView // Adicione esta linha


    // --- Variáveis de Estado do Jogo ---
    private var score = 0
    private var timer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 90000L // 90 segundos
    private var isGameRunning = false

    // --- Variáveis de Jogo da Memória ---
    private var cardBackDrawableId: Int = 0
    private var flippedCards: MutableList<ImageView> = mutableListOf()
    private var flippedItems: MutableList<String> = mutableListOf()
    private val matchedImageViews: MutableSet<ImageView> = mutableSetOf()
    private val mainLooper = Looper.getMainLooper()
    private var matchedPairsCount = 0
    private var currentRoundItems: MutableList<String> = mutableListOf()

    // NOVA VARIÁVEL PARA RASTREAR ACERTOS CONSECUTIVOS
    private var consecutiveMatches: Int = 0


    // Lista completa de todos os pares disponíveis para o jogo
    private val allMemoryPairs = listOf(
        Pair("ballerina_cappuccina", "ballerina_cappuccina01"),
        Pair("bombardiro_crocodilo", "bombardiro_crocodilo01"),
        Pair("brr_brr_patapim", "brr_brr_patapim01"),
        Pair("cappuccino_assassino", "cappuccino_assassino01"),
        Pair("espressona_signora", "espressona_signora01"),
        Pair("odindin", "odindin01"),
        Pair("tralalero_tralala", "tralalero_tralala01"),
        Pair("tung", "tung01"),
        Pair("tripy_trophy", "tripy_trophy01"),
        Pair("trippi_troppi", "trippi_troppi01"),
        Pair("matteooooooooooooo", "matteooooooooooooo01"),
        Pair("spie", "spie01"),
        Pair("ta", "ta01"),
        Pair("lavacasaturno_saturnita", "lavacasaturno_saturnita01"),
        Pair("garamara", "garamara01"),
        Pair("chimpanzini_bananini", "chimpanzini_bananini01"),
        Pair("bombombini_gusini", "bombombini_gusini01"),
        Pair("boneca_ambalabu", "boneca_ambalabu01"),
        Pair("lirili_larila_elephant", "lirili_larila_elephant01")
    )

    // --- Variáveis de Áudio ---
    private var backgroundMusicPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var soundIdClick: Int = 0
    private var soundIdMatch: Int = 0
    private var soundIdNoMatch: Int = 0
    private var soundIdGameOver: Int = 0
    private var soundIdWow: Int = 0
    // Variável para o MediaPlayer do tick-tack
    private var mediaPlayer: MediaPlayer? = null

    // Variáveis de controle de níveis
    private var currentLevel = 0
    private val levelCardCounts = listOf(12, 16, 20) // Quantidade de cartas por nível
    private val levelPairCounts = listOf(6, 8, 10) // Quantidade de pares por nível (metade das cartas)


    // --- Variáveis de Preferências e Senha ---
    private val PREFS_NAME = "MemoryGamePrefs"
    private val KEY_HIGH_SCORES = "high_scores"
    private val ADMIN_PASSWORD = "09072014" // <-- MUDE ESTA SENHA PARA UMA DE SUA ESCOLHA!

    // Variável de estado para a funcionalidade "Revelar Cartas" no início do nível
    private val KEY_QUICK_PEEK_ENABLED = "quick_peek_enabled"
    private var isQuickPeekEnabled: Boolean = true // Começa ativado por padrão


    // --- Método onCreate: Onde tudo é inicializado ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialização dos elementos de UI
        gameTitle = findViewById(R.id.gameTitle)
        scoreText = findViewById(R.id.scoreText)
        timerText = findViewById(R.id.timerText)
        restartGameButton = findViewById(R.id.restartGameButton)
        gameEndRestartButton = findViewById(R.id.gameEndRestartButton)

        gameRulesText = findViewById(R.id.gameRulesText)
        gameStatusLayout = findViewById(R.id.game_status_layout)
        cardGridLayout = findViewById(R.id.card_grid_layout)
        inGameAdminButtonsLayout = findViewById(R.id.in_game_admin_buttons_layout)

        // INICIALIZAÇÃO DO KONFETTIVIEW
        konfettiView = findViewById(R.id.konfetti_view)


        // Inicialização das ImageViews das cartas
        cardImages = listOf(
            findViewById(R.id.card1), findViewById(R.id.card2), findViewById(R.id.card3),
            findViewById(R.id.card4), findViewById(R.id.card5), findViewById(R.id.card6),
            findViewById(R.id.card7), findViewById(R.id.card8), findViewById(R.id.card9),
            findViewById(R.id.card10), findViewById(R.id.card11), findViewById(R.id.card12),
            findViewById(R.id.card13), findViewById(R.id.card14), findViewById(R.id.card15),
            findViewById(R.id.card16), findViewById(R.id.card17), findViewById(R.id.card18),
            findViewById(R.id.card19), findViewById(R.id.card20)
        )

        // Define a distância da câmera para todas as cartas para um efeito 3D
        val distance = 8000 * resources.displayMetrics.density
        cardImages.forEach { it.cameraDistance = distance }


        // Inicialização dos elementos de UI do Placar
        highScoreTitle = findViewById(R.id.highScoreTitle)
        highScore1 = findViewById(R.id.highScore1)
        highScore2 = findViewById(R.id.highScore2)
        highScore3 = findViewById(R.id.highScore3)
        highScore4 = findViewById(R.id.highScore4)
        highScore5 = findViewById(R.id.highScore5)
        resetScoresButton = findViewById(R.id.resetScoresButton)
        addMinuteButton = findViewById(R.id.addMinuteButton)
        togglePeekButton = findViewById(R.id.togglePeekButton)
        revealCardsInGameButton = findViewById(R.id.revealCardsInGameButton)
        goToPhase3Button = findViewById(R.id.goToPhase3Button) // NOVO: Inicializa o botão

        // --- Carrega a preferência de "Revelar Cartas" ao iniciar o app ---
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isQuickPeekEnabled = sharedPrefs.getBoolean(KEY_QUICK_PEEK_ENABLED, true)
        updatePeekButtonText()


        // Listener para o botão "Começar Jogo" (tela inicial)
        restartGameButton.setOnClickListener {
            soundPool?.play(soundIdClick, 1.0f, 1.0f, 0, 0, 1.0f)
            startGame() // Chama a função que inicia o jogo do primeiro nível
        }

        // Listener para o botão "Reiniciar Jogo" (tela de fim de jogo)
        gameEndRestartButton.setOnClickListener {
            soundPool?.play(soundIdClick, 1.0f, 1.0f, 0, 0, 1.0f)
            startGame() // Chama a função que inicia um novo jogo do primeiro nível
        }

        // Listener para o botão de Zerar Placar
        resetScoresButton.setOnClickListener {
            soundPool?.play(soundIdClick, 1.0f, 1.0f, 0, 0, 1.0f)
            showPasswordDialog()
        }

        // Listener para o botão "Adicionar 1 Minuto"
        addMinuteButton.setOnClickListener {
            soundPool?.play(soundIdClick, 1.0f, 1.0f, 0, 0, 1.0f)
            showAddMinutePasswordDialog()
        }

        // Listener para o botão de Ativar/Desativar "Revelar Cartas" (configuração)
        togglePeekButton.setOnClickListener {
            soundPool?.play(soundIdClick, 1.0f, 1.0f, 0, 0, 1.0f)
            showTogglePeekPasswordDialog()
        }

        // NOVO: Listener para o botão "Revelar Cartas" durante o jogo
        revealCardsInGameButton.setOnClickListener {
            soundPool?.play(soundIdClick, 1.0f, 1.0f, 0, 0, 1.0f)
            showRevealCardsInGamePasswordDialog()
        }

        // NOVO: Listener para o botão "Ir para Fase 3"
        goToPhase3Button.setOnClickListener {
            soundPool?.play(soundIdClick, 1.0f, 1.0f, 0, 0, 1.0f)
            showGoToPhase3PasswordDialog() // Chama o diálogo de senha para a Fase 3
        }


        // Inicializa o ID do drawable do verso da carta
        cardBackDrawableId = resources.getIdentifier("card_back", "drawable", packageName)
        if (cardBackDrawableId == 0) {
            Log.e("MemoryGame", "Erro: card_back.png não encontrado na pasta drawable.")
            Toast.makeText(this, "Erro: Imagem card_back.png não encontrada!", Toast.LENGTH_LONG)
                .show()
        }

        // --- Configuração de Áudio ---
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundIdClick = soundPool?.load(this, R.raw.click_sound, 1) ?: 0
        soundIdMatch = soundPool?.load(this, R.raw.match_sound, 1) ?: 0
        soundIdNoMatch = soundPool?.load(this, R.raw.no_match_sound, 1) ?: 0
        soundIdGameOver = soundPool?.load(this, R.raw.game_over_sound, 1) ?: 0
        soundIdWow = soundPool?.load(this, R.raw.wow_sound, 1) ?: 0

        backgroundMusicPlayer = MediaPlayer.create(this, R.raw.background_music)
        backgroundMusicPlayer?.isLooping = true
        backgroundMusicPlayer?.setVolume(0.5f, 0.5f)
        // --- FIM Configuração de Áudio ---

        // Define o estado inicial da UI para "pronto para começar"
        setupInitialUIState()

        pointsChangeText = findViewById(R.id.pointsChangeText) // Inicialize o novo TextView

        // Inicialize o MediaPlayer (faça isso uma vez)
        mediaPlayer = MediaPlayer.create(this, R.raw.tick_tack_sound) // Você precisará criar o arquivo tick_tack_sound.mp3/wav em res/raw
        mediaPlayer?.isLooping = false // Não repete automaticamente

    }



    // --- NOVO MÉTODO: Configura o estado da UI para a tela inicial ---
    private fun setupInitialUIState() {
        gameTitle.text = "Jogo da Memória"

        // Esconde os elementos do jogo
        gameStatusLayout.visibility = View.GONE
        cardGridLayout.visibility = View.GONE
        inGameAdminButtonsLayout.visibility = View.GONE // Esconde o layout de botões em jogo

        // Esconde o botão de reiniciar do fim do jogo
        gameEndRestartButton.visibility = View.GONE

        // Mostra o texto das regras, botão de iniciar e placar
        gameRulesText.visibility = View.VISIBLE
        restartGameButton.visibility = View.VISIBLE
        restartGameButton.text = "Começar Jogo"

        // Mostra o placar e os botões de administração (incluindo o novo de configuração)
        showHighScoresElements(true)
        togglePeekButton.visibility = View.VISIBLE // MOSTRA o botão de peek no menu principal
        goToPhase3Button.visibility = View.VISIBLE // NOVO: MOSTRA o botão Ir para Fase 3
        displayHighScores() // Atualiza o placar

        // *** AJUSTE AQUI PARA A COR DA PONTUAÇÃO INICIAL ***
        score = 0 // A pontuação sempre começa em 0
        scoreText.text = "Pontos: $score"
        // Defina a cor para quando a pontuação é zero
        scoreText.setTextColor(ContextCompat.getColor(this, R.color.score_zero_color))

    }

    // NOVO MÉTODO: Configura o estado da UI para o jogo em andamento
    private fun setupGameRunningUIState() {
        // Esconde elementos da tela inicial
        gameRulesText.visibility = View.GONE
        restartGameButton.visibility = View.GONE
        gameEndRestartButton.visibility = View.GONE // Esconde o botão "Reiniciar Jogo" do fim do jogo
        showHighScoresElements(false) // Esconde o placar e o botão de zerar
        togglePeekButton.visibility = View.GONE // OCULTA o botão de peek (configuração) DURANTE o jogo
        goToPhase3Button.visibility = View.GONE // NOVO: OCULTA o botão Ir para Fase 3 DURANTE o jogo


        // Mostra elementos do jogo
        gameStatusLayout.visibility = View.VISIBLE
        cardGridLayout.visibility = View.VISIBLE
        inGameAdminButtonsLayout.visibility = View.VISIBLE // MOSTRA o layout de botões em jogo
    }

    // NOVO MÉTODO: Configura o estado da UI para o fim do jogo (com placar e botão reiniciar)
    private fun setupEndGameUI() {
        // Esconde os elementos do jogo
        gameStatusLayout.visibility = View.GONE
        cardGridLayout.visibility = View.GONE
        inGameAdminButtonsLayout.visibility = View.GONE // Esconde o layout de botões em jogo

        // Mostra o botão de reiniciar do fim do jogo
        gameEndRestartButton.visibility = View.VISIBLE

        // Mostra o placar e os botões de administração (incluindo o novo de configuração)
        showHighScoresElements(true)
        togglePeekButton.visibility = View.VISIBLE // MOSTRA o botão de peek na tela de fim de jogo
        goToPhase3Button.visibility = View.VISIBLE // NOVO: MOSTRA o botão Ir para Fase 3 na tela de fim de jogo
        displayHighScores() // Atualiza o placar
    }

    // Método auxiliar para controlar a visibilidade dos elementos do placar
    private fun showHighScoresElements(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        highScoreTitle.visibility = visibility
        highScore1.visibility = visibility
        highScore2.visibility = visibility
        highScore3.visibility = visibility
        highScore4.visibility = visibility
        highScore5.visibility = visibility
        resetScoresButton.visibility = visibility
        // inGameAdminButtonsLayout, togglePeekButton e goToPhase3Button são controlados separadamente
    }


    // --- Funções do Jogo ---

    // Esta é a função principal que inicia um NOVO JOGO COMPLETO do zero.
    private fun startGame() {
        currentLevel = 0 // Garante que sempre comece no primeiro nível
        score = 0 // Zera a pontuação no início de um novo jogo
        startGameForCurrentLevel() // Inicia o jogo para o primeiro nível
        consecutiveMatches = 0 // Zera o contador de acertos consecutivos ao iniciar um novo jogo
    }

    // Esta função gerencia o início de CADA NÍVEL.
    // Adicionado um parâmetro para controlar se o timer deve ser resetado para 90s.
    private fun startGameForCurrentLevel(resetTimer: Boolean = true) { // Default é true para novos níveis
        isGameRunning = true

        if (resetTimer) {
            timeLeftInMillis = 90000L // Reseta para 90 segundos apenas se estiver começando um novo nível
        }

        // Reinicia variáveis do jogo da memória para o NOVO NÍVEL/RODADA
        matchedPairsCount = 0
        flippedCards.clear()
        flippedItems.clear()
        // FIX para Problema 1: Garante que matchedImageViews seja limpo a cada novo jogo/nível/rodada.
        matchedImageViews.clear() // Limpa todas as cartas previamente combinadas

        scoreText.text = "Pontos: $score" // Atualiza a pontuação exibida

        // Define o estado da UI para "jogo em andamento"
        setupGameRunningUIState()

        val totalPairsForLevel = levelPairCounts[currentLevel]

        // Inicia a música de fundo (se já não estiver tocando)
        backgroundMusicPlayer?.start()

        startTimer() // O timer será iniciado com o 'timeLeftInMillis' atual (resetado ou estendido)
        setupNewRoundForLevel(totalPairsForLevel) // Passa a quantidade de pares para este nível
    }

    // Inicia o CountDownTimer
    private fun startTimer() {
        timer?.cancel() // Cancela qualquer timer anterior

        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                val seconds = millisUntilFinished / 1000
                timerText.text = "Tempo: ${seconds}s"

                // Lógica do tick-tack para os últimos 10 segundos
                if (seconds <= 10) {
                    timerText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red_points)) // Muda para vermelho
                    // Toca o som do tick-tack APENAS se ele não estiver tocando no momento
                    // Isso evita sobreposição se o som for mais longo que 1 segundo
                    if (mediaPlayer?.isPlaying == false || mediaPlayer == null) { // Verifica se não está tocando ou se é nulo (fallback)
                        try {
                            mediaPlayer?.start()
                        } catch (e: IllegalStateException) {
                            Log.e("MemoryGame", "Erro ao tocar tick-tack: ${e.message}")
                            // Tenta recriar o mediaPlayer se houver um erro de estado (raro)
                            mediaPlayer = MediaPlayer.create(this@MainActivity, R.raw.tick_tack_sound)
                            mediaPlayer?.isLooping = false
                            mediaPlayer?.start()
                        }
                    }
                } else {
                    // Volta à cor normal (use colorAccent que você já tem no projeto)
                    timerText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.timer_normal_color))
                    // Se o som estava tocando (nos últimos 10s) e agora não está mais, pare-o.
                    // Isso é importante se o usuário adicionar tempo e sair da zona dos 10s.
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                        mediaPlayer?.seekTo(0) // Volta ao início para a próxima vez
                    }
                }
            }

            override fun onFinish() {
                isGameRunning = false
                timerText.text = "Tempo: 0s"
                cardImages.forEach { it.setOnClickListener(null) } // Desativa cliques

                backgroundMusicPlayer?.pause()
                backgroundMusicPlayer?.seekTo(0) // Volta para o início da música
                soundPool?.play(soundIdGameOver, 1.0f, 1.0f, 0, 0, 1.0f)

                // *** ADICIONADO: Garante que o tick-tack pare quando o timer termina ***
                mediaPlayer?.pause()
                mediaPlayer?.seekTo(0) // Volta para o início do som para a próxima vez

                // Chama a função para mostrar o dialog e salvar a pontuação
                showNameInputDialog(score)
                // setupEndGameUI() será chamada após o diálogo.
            }
        }.start()
    }

    // Configura uma nova rodada para o nível atual
    private fun setupNewRoundForLevel(pairsForThisRound: Int) {
        if (!isGameRunning) return

        matchedPairsCount = 0 // Reinicia contador de pares encontrados para a nova rodada dentro do nível
        flippedCards.clear()
        flippedItems.clear()
        // matchedImageViews.clear() foi movido para startGameForCurrentLevel()

        // Seleciona X pares aleatórios do allMemoryPairs para o nível atual
        val selectedPairs = allMemoryPairs.shuffled().take(pairsForThisRound)
        currentRoundItems.clear() // Limpa antes de adicionar novos itens
        selectedPairs.forEach {
            currentRoundItems.add(it.first)
            currentRoundItems.add(it.second)
        }
        currentRoundItems.shuffle() // Embaralha as cartas para este nível

        Log.d("MemoryGame", "NOVA RODADA GERADA. Itens: $currentRoundItems")

        val cardBackResId = resources.getIdentifier("card_back", "drawable", packageName)
        if (cardBackResId == 0) {
            Log.e(
                "MemoryGame",
                "ERRO: card_back.png não encontrado. Certifique-se de que está em res/drawable."
            )
            Toast.makeText(this, "Erro: Imagem card_back.png ausente!", Toast.LENGTH_LONG).show()
            return
        }

        // --- CORREÇÃO PRINCIPAL: Anexar OnClickListener a todas as cartas relevantes ---
        // Primeiro, configura e anexa os listeners para todas as cartas do nível
        for (i in cardImages.indices) {
            val cardView = cardImages[i]
            if (i < currentRoundItems.size) {
                val item = currentRoundItems[i] // Obtém o item para esta carta

                cardView.visibility = View.VISIBLE
                cardView.tag = item
                cardView.rotationY = 0f
                cardView.alpha = 1.0f
                cardView.setImageResource(cardBackResId) // Começam todas viradas para baixo

                // SEMPRE ANEXA O CLICK LISTENER AQUI
                cardView.setOnClickListener { view ->
                    handleCardClick(view as ImageView, item)
                }
                // Desabilita temporariamente os cliques. Eles serão reabilitados pela animação de peek
                // ou imediatamente se o peek estiver desativado.
                cardView.isClickable = false
            } else {
                cardView.visibility = View.GONE
                cardView.setOnClickListener(null) // Remove o listener para cartas escondidas
                cardView.isClickable = false // Garante que cartas escondidas não sejam clicáveis
            }
        }

        // Agora, gerencia o estado clicável com base na animação de peek
        if (isQuickPeekEnabled) {
            performQuickPeekAnimation(currentRoundItems.size, cardBackResId) {
                // Cliques são reabilitados dentro da ação final de performQuickPeekAnimation
                // para cartas não combinadas.
            }
        } else {
            // Se o Quick Peek estiver DESABILITADO, habilita cliques imediatamente para todas as cartas visíveis
            for (i in 0 until currentRoundItems.size) {
                val cardView = cardImages[i]
                if (!matchedImageViews.contains(cardView)) { // Apenas habilita se a carta ainda não foi combinada
                    cardView.isClickable = true
                }
            }
        }
    }


    // Lógica para verificar a conclusão do jogo/nível
    private fun checkGameCompletion() {
        val totalPairsForCurrentLevel = levelPairCounts[currentLevel]

        if (matchedPairsCount == totalPairsForCurrentLevel) {
            // Ações gerais para completar qualquer nível/rodada
            soundPool?.play(soundIdWow, 1.0f, 1.0f, 0, 0, 1.0f)
            Toast.makeText(this, "Nível Completo!", Toast.LENGTH_LONG).show()

            score += 20 // Bônus de pontos por completar qualquer nível/rodada
            scoreText.text = "Pontos: $score"

            if (currentLevel < levelCardCounts.size - 1) { // Se não é o último nível
                // Avança para o próximo nível
                currentLevel++
                Handler(mainLooper).postDelayed({
                    // Ao mover para um novo nível, queremos resetar o timer para 90s
                    startGameForCurrentLevel(resetTimer = true)
                }, 1500)
            } else { // É o último nível (currentLevel == 2) - lidar com a rodada bônus
                Toast.makeText(this, "Nível Máximo Completo! Nova rodada bônus!", Toast.LENGTH_LONG).show()

                // Adiciona 20 segundos ao tempo existente
                timeLeftInMillis += 20000L // Adiciona 20 segundos (20.000 milissegundos)
                timer?.cancel() // Cancela o timer atual
                startTimer() // Reinicia o timer com o tempo estendido

                // Crucialmente, limpa matchedImageViews e configura uma nova rodada para o *mesmo* nível
                Handler(mainLooper).postDelayed({
                    matchedImageViews.clear() // Limpa o estado combinado para a nova rodada
                    setupNewRoundForLevel(totalPairsForCurrentLevel) // Configura uma nova rodada para o nível atual (último)
                }, 1500)
            }
        }
    }

    // Função para tratar cliques nas cartas do jogo da memória
    private fun handleCardClick(clickedImageView: ImageView, clickedItem: String) {
        // Bloqueia cliques se o jogo não está rodando, se a carta já está virada, ou se duas cartas já estão viradas
        if (!isGameRunning || flippedCards.contains(clickedImageView) || flippedCards.size == 2) {
            return
        }

        if (flippedCards.size == 0) {
            soundPool?.play(soundIdClick, 1.0f, 1.0f, 0, 0, 1.0f)
        }

        val itemResId = resources.getIdentifier(clickedItem, "drawable", packageName)

        // Desabilita o clique na carta IMEDIATAMENTE para evitar cliques múltiplos durante a animação
        clickedImageView.isClickable = false

        // Inicia a animação de virar a carta para frente (mostrando a imagem)
        flipCardAnimation(clickedImageView, itemResId, true) {
            // Este bloco é executado APÓS a animação de virada para frente CADA CARTA virada.
            flippedCards.add(clickedImageView)
            flippedItems.add(clickedItem)

            // Se duas cartas foram viradas, verifica se são um par
            if (flippedCards.size == 2) {
                val item1 = flippedItems.first()
                val item2 = flippedItems.last()
                val imageView1 = flippedCards.first()
                val imageView2 = flippedCards.last()

                // Verifica se as duas imagens formam um par válido na lista allMemoryPairs
                val isMatch = allMemoryPairs.any { (it.first == item1 && it.second == item2) || (it.second == item1 && it.first == item2) }

                if (isMatch) {
                    soundPool?.play(soundIdMatch, 1.0f, 1.0f, 0, 0, 1.0f)
                    val pointsEarned = 5 // Pontos ganhos por um par
                    score += pointsEarned
                    scoreText.text = "Pontos: $score"
                    Toast.makeText(this, "Par Encontrado!", Toast.LENGTH_SHORT).show()

                    scoreText.text = "Pontos: $score" // Atualiza o texto da pontuação primeiro

                    if (score > 0) {
                        scoreText.setTextColor(ContextCompat.getColor(this, R.color.green_strong))
                    } else if (score < 0) {
                        scoreText.setTextColor(ContextCompat.getColor(this, R.color.red_points))
                    } else { // score == 0
                        scoreText.setTextColor(
                            ContextCompat.getColor(
                                this,
                                R.color.score_zero_color
                            )
                        )
                    }
                    // *** ADICIONE ESTA LINHA AQUI PARA MOSTRAR O POPUP DE PONTOS POSITIVOS ***
                    showPointsChange(pointsEarned)

                    matchedPairsCount++
                    matchedImageViews.add(imageView1) // ADICIONADO: Marca a carta como combinada
                    matchedImageViews.add(imageView2) // ADICIONADO: Marca a carta como combinada

                    // Incrementa o contador de acertos consecutivos
                    consecutiveMatches++

                    // Verifica se 3 pares consecutivos foram acertados
                    if (consecutiveMatches == 3) {
                        showConfettiEffect(imageView1, imageView2) // Dispara o efeito de confetes
                        consecutiveMatches = 0 // Reseta o contador
                    }


                    flippedCards.clear()
                    flippedItems.clear()

                    // Chama a função de verificação de conclusão do nível/jogo
                    checkGameCompletion()

                } else {
                    // Não é um par
                    soundPool?.play(soundIdNoMatch, 1.0f, 1.0f, 0, 0, 1.0f)
                    val pointsLost = -2 // Pontos perdidos por um erro
                    score += pointsLost
                    scoreText.text = "Pontos: $score"
                    Toast.makeText(this, "Par Incorreto!", Toast.LENGTH_SHORT).show()

                    scoreText.text = "Pontos: $score" // Atualiza o texto da pontuação

                     if (score > 0) {
                         scoreText.setTextColor(ContextCompat.getColor(this, R.color.green_strong))
                     } else if (score < 0) {
                         scoreText.setTextColor(ContextCompat.getColor(this, R.color.red_points))
                     } else { // score == 0
                         scoreText.setTextColor(ContextCompat.getColor(this, R.color.score_zero_color))
                     }

                    // *** ADICIONE ESTA LINHA AQUI PARA MOSTRAR O POPUP DE PONTOS NEGATIVOS ***
                    showPointsChange(pointsLost)

                    // Reseta o contador de acertos consecutivos se não for um par
                    consecutiveMatches = 0

                    // Atraso antes de virar as cartas de volta
                    Handler(mainLooper).postDelayed({
                        // Animação de virar as cartas de volta para o verso
                        flipCardAnimation(imageView1, cardBackDrawableId, false) {
                            imageView1.isClickable = true // Torna a carta clicável novamente após a animação de voltar
                        }
                        flipCardAnimation(imageView2, cardBackDrawableId, false) {
                            imageView2.isClickable = true // Torna a carta clicável novamente após a animação de voltar
                            // Limpa as cartas viradas apenas após AMBAS as animações de retorno
                            // e as cartas estarem clicáveis novamente. Isso evita problemas de sincronização.
                            flippedCards.clear()
                            flippedItems.clear()
                        }
                    }, 1000) // Atraso de 1 segundo para o jogador ver as cartas antes de virarem de volta
                }
            }
        }
    }


    /**
     * Realiza a animação de virada de uma carta.
     * @param cardView A ImageView da carta a ser animada.
     * @param newImageResId O ID do recurso da nova imagem para a carta (frente ou verso).
     * @param isFlippingToFront Se true, a carta está virando do verso para a frente. Se false, da frente para o verso.
     * @param onAnimationComplete Um callback opcional a ser executado quando a animação estiver completa.
     */
    private fun flipCardAnimation(cardView: ImageView, newImageResId: Int, isFlippingToFront: Boolean, onAnimationComplete: () -> Unit = {}) {

        // Primeira metade da animação (vira até 90 graus, para ficar de lado)
        cardView.animate()
            .rotationY(if (isFlippingToFront) 90f else -90f) // Gira para 90 ou -90 (parece uma linha)
            .setDuration(200) // Duração da primeira metade
            .withEndAction { // Quando a primeira metade termina
                // Troca a imagem
                cardView.setImageResource(newImageResId)

                // Segunda metade da animação (vira de 90 ou -90 para 0, mostrando a nova face)
                // Para que a animação pareça contínua, resetamos a rotação para -90 ou 90
                // antes de animar de volta para 0.
                cardView.rotationY = if (isFlippingToFront) -90f else 90f // Posição "invertida" para começar a segunda parte

                cardView.animate()
                    .rotationY(0f) // Gira de volta para a posição original (0 graus)
                    .setDuration(200) // Duração da segunda metade
                    .setListener(null) // Limpa listener anterior para evitar chamadas duplicadas
                    .withEndAction {
                        onAnimationComplete() // Chama o callback quando a animação termina
                    }
                    .start()
            }
            .start()
    }

    /**
     * Realiza a animação de "revelar" rapidamente todas as cartas no início do nível
     * ou durante o jogo (para a função de admin).
     * @param totalCardsInLevel O número total de cartas no nível atual.
     * @param cardBackResId O ID do recurso do drawable do verso da carta.
     * @param onAnimationComplete Um callback opcional a ser executado quando a animação estiver completa.
     */
    private fun performQuickPeekAnimation(totalCardsInLevel: Int, cardBackResId: Int, onAnimationComplete: () -> Unit = {}) {
        // Desabilita todos os cliques nas cartas enquanto a animação ocorre
        cardImages.forEach { it.isClickable = false }

        // Animação para virar as cartas para a frente (mostrando a imagem real)
        for (i in 0 until totalCardsInLevel) {
            val cardView = cardImages[i]
            val item = currentRoundItems[i]
            val resId = resources.getIdentifier(item, "drawable", packageName)

            if (matchedImageViews.contains(cardView)) {
                // Se a carta já está combinada, não faz animação, apenas garante que esteja visível e correta
                cardView.setImageResource(resId)
                cardView.visibility = View.VISIBLE
                cardView.rotationY = 0f
                cardView.alpha = 1.0f
                cardView.tag = item
                // Já está não clicável se foi combinada, não precisa habilitar de novo
            } else {
                // Anima as cartas não combinadas para a frente
                flipCardAnimation(cardView, resId, true)
                cardView.visibility = View.VISIBLE // Garante que esteja visível
                cardView.tag = item // Define a tag com o item real para correspondência futura
                cardView.alpha = 1.0f // Garante que não esteja transparente
            }
        }

        // Atraso para as cartas permanecerem visíveis
        Handler(mainLooper).postDelayed({
            // Animação para virar as cartas de volta para o verso
            for (i in 0 until totalCardsInLevel) {
                val cardView = cardImages[i]
                if (!matchedImageViews.contains(cardView)) { // Apenas vira de volta as não combinadas
                    flipCardAnimation(cardView, cardBackResId, false) {
                        // Re-habilita cliques apenas para cartas que não são pares combinados
                        cardView.isClickable = true
                    }
                }
            }
            onAnimationComplete() // Chama o callback principal após todas as animações
        }, 2000) // Cartas visíveis por 2 segundos
    }



    // NOVA FUNÇÃO: Dispara o efeito de confetes das cartas
    private fun showConfettiEffect(card1: ImageView, card2: ImageView) {
        // Cores dos confetes
        val colors = intArrayOf(
            ContextCompat.getColor(this, R.color.colorPrimary),    // CORRIGIDO AQUI
            ContextCompat.getColor(this, R.color.colorAccent),     // E AQUI
            0xF08080FF.toInt(), // Um tom de vermelho/rosa claro
            0xFFD700FF.toInt()  // Dourado
        )



        // Calcula a posição central de cada carta
        val location1 = IntArray(2)
        card1.getLocationOnScreen(location1)
        val card1CenterX = location1[0] + card1.width / 2f
        val card1CenterY = location1[1] + card1.height / 2f

        val location2 = IntArray(2)
        card2.getLocationOnScreen(location2)
        val card2CenterX = location2[0] + card2.width / 2f
        val card2CenterY = location2[1] + card2.height / 2f

        // Confetes da primeira carta
        konfettiView.build()
            .addColors(*colors)
            .setDirection(0.0, 359.0) // Todas as direções
            .setSpeed(1f, 5f) // Velocidade das partículas
            .setFadeOutEnabled(true) // Partículas desaparecem
            .setTimeToLive(2000L) // Tempo de vida das partículas (2 segundos)
            .addShapes(Shape.Square, Shape.Circle) // Formatos dos confetes
            .addSizes(Size(8)) // Tamanho dos confetes
            .setPosition(card1CenterX, card1CenterY) // Posição de origem
            .burst(80) // Dispara 80 partículas de uma vez (efeito de explosão)

        // Confetes da segunda carta
        konfettiView.build()
            .addColors(*colors)
            .setDirection(0.0, 359.0)
            .setSpeed(1f, 5f)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000L)
            .addShapes(Shape.Square, Shape.Circle)
            .addSizes(Size(8))
            .setPosition(card2CenterX, card2CenterY)
            .burst(80)
    }
    // --- NOVA FUNÇÃO: Exibe a mudança de pontos com animação ---
    private fun showPointsChange(points: Int) {
        pointsChangeText.text = if (points > 0) "+$points" else "$points"
        pointsChangeText.setTextColor(
            ContextCompat.getColor(this, if (points > 0) R.color.green_strong else R.color.red_points)
        )

        // Resetar propriedades para garantir que a animação comece do zero
        pointsChangeText.alpha = 0.0f
        pointsChangeText.scaleX = 1.0f
        pointsChangeText.scaleY = 1.0f
        pointsChangeText.translationY = 0f // <-- MUDOU AQUI: Começa na linha do texto principal

        val animatorSet = AnimatorSet()

        // Animação de subida e fade-in (primeira parte: aparece e cresce um pouco)
        val fadeIn = ObjectAnimator.ofFloat(pointsChangeText, View.ALPHA, 0.0f, 1.0f)
        val moveUp = ObjectAnimator.ofFloat(pointsChangeText, View.TRANSLATION_Y, 0f, -40f) // Sobe 40dp do ponto 0
        val scaleUpX = ObjectAnimator.ofFloat(pointsChangeText, View.SCALE_X, 1.0f, 1.2f)
        val scaleUpY = ObjectAnimator.ofFloat(pointsChangeText, View.SCALE_Y, 1.0f, 1.2f)

        // Animação de fade-out e volta ao tamanho normal (segunda parte: desaparece e volta ao tamanho)
        val fadeOut = ObjectAnimator.ofFloat(pointsChangeText, View.ALPHA, 1.0f, 0.0f)
        val scaleDownX = ObjectAnimator.ofFloat(pointsChangeText, View.SCALE_X, 1.2f, 1.0f)
        val scaleDownY = ObjectAnimator.ofFloat(pointsChangeText, View.SCALE_Y, 1.2f, 1.0f)
        // ESTA É A LINHA CRÍTICA PARA O POSICIONAMENTO: ele retorna à sua posição inicial (0f)
        val returnToStartTranslationY = ObjectAnimator.ofFloat(pointsChangeText, View.TRANSLATION_Y, -40f, 0f)


        // Configuração da animação
        animatorSet.play(fadeIn).with(moveUp).with(scaleUpX).with(scaleUpY) // Fase 1: Sobe, aparece e cresce
        // Fase 2: Depois de um atraso, ele desaparece, volta ao tamanho e retorna à posição inicial
        animatorSet.play(fadeOut).with(scaleDownX).with(scaleDownY).with(returnToStartTranslationY).after(400)

        animatorSet.duration = 800 // Duração total da animação
        animatorSet.interpolator = AccelerateDecelerateInterpolator() // Suaviza o movimento
        animatorSet.start()
    }

    // --- Funções do Placar ---

    private fun saveHighScore(playerName: String, newScore: Int) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentHighScoresString = sharedPrefs.getString(KEY_HIGH_SCORES, "")
        val highScoresList = if (currentHighScoresString.isNullOrEmpty()) {
            mutableListOf()
        } else {
            currentHighScoresString.split(",").toMutableList()
        }

        highScoresList.add("$newScore:$playerName")
        highScoresList.sortByDescending { it.split(":")[0].toInt() }
        val top5Scores = highScoresList.take(5)

        val editor = sharedPrefs.edit()
        editor.putString(KEY_HIGH_SCORES, top5Scores.joinToString(","))
        editor.apply()
        displayHighScores()
    }

    private fun displayHighScores() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val highScoresString = sharedPrefs.getString(KEY_HIGH_SCORES, "")
        val highScoresEntries = if (highScoresString.isNullOrEmpty()) {
            listOf()
        } else {
            highScoresString.split(",")
        }

        val scoreTextViews = listOf(highScore1, highScore2, highScore3, highScore4, highScore5)

        for (i in 0 until 5) {
            if (i < highScoresEntries.size) {
                val entryParts = highScoresEntries[i].split(":")
                val score = entryParts.getOrElse(0) { "0" }
                val name = entryParts.getOrElse(1) { "---" }
                scoreTextViews[i].text = "${i + 1}. $name: $score pontos"
            } else {
                scoreTextViews[i].text = "${i + 1}. ---"
            }
        }
    }

    // --- Funções de Senha e Zerar Placar ---

    private fun showPasswordDialog() { // Usado para zerar placar
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Zerar Placar")
        builder.setMessage("Digite a senha para zerar o placar:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Confirmar") { dialog, _ ->
            val enteredPassword = input.text.toString()
            if (enteredPassword == ADMIN_PASSWORD) {
                resetHighScores()
                Toast.makeText(this, "Placar zerado com sucesso!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Senha incorreta.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // Diálogo de senha para adicionar tempo
    private fun showAddMinutePasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Adicionar Tempo")
        builder.setMessage("Digite a senha para adicionar 1 minuto ao tempo:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Confirmar") { dialog, _ ->
            val enteredPassword = input.text.toString()
            if (enteredPassword == ADMIN_PASSWORD) {
                addMinuteToTimer()
                Toast.makeText(this, "1 minuto adicionado!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Senha incorreta.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // Diálogo de senha para ativar/desativar "Revelar Cartas" (configuração inicial)
    private fun showTogglePeekPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Configurar Revelar Cartas")
        builder.setMessage("Digite a senha para ${if (isQuickPeekEnabled) "desativar" else "ativar"} a função de revelar cartas no início de cada nível:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Confirmar") { dialog, _ ->
            val enteredPassword = input.text.toString()
            if (enteredPassword == ADMIN_PASSWORD) {
                toggleQuickPeekFeature() // Chama a função para alternar o estado
                Toast.makeText(this, "Função Revelar Cartas ${if (isQuickPeekEnabled) "ativada" else "desativada"}.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Senha incorreta.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // NOVO: Diálogo de senha para "Revelar Cartas" durante o jogo
    private fun showRevealCardsInGamePasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Revelar Cartas")
        builder.setMessage("Digite a senha para revelar as cartas por 2 segundos:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Confirmar") { dialog, _ ->
            val enteredPassword = input.text.toString()
            if (enteredPassword == ADMIN_PASSWORD) {
                if (isGameRunning) {
                    revealCardsDuringGame() // Chama a função para revelar as cartas
                    Toast.makeText(this, "Cartas reveladas por 2 segundos!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "O jogo não está em andamento.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Senha incorreta.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }


    // Função para alternar o estado da funcionalidade "Revelar Cartas" (configuração inicial)
    private fun toggleQuickPeekFeature() {
        isQuickPeekEnabled = !isQuickPeekEnabled // Inverte o estado
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putBoolean(KEY_QUICK_PEEK_ENABLED, isQuickPeekEnabled) // Salva o novo estado
        editor.apply()
        updatePeekButtonText() // Atualiza o texto do botão na UI
    }

    // Função para atualizar o texto do botão "Revelar Cartas" (configuração inicial)
    private fun updatePeekButtonText() {
        if (isQuickPeekEnabled) {
            togglePeekButton.text = "Desativar Revelar Cartas"
        } else {
            togglePeekButton.text = "Ativar Revelar Cartas"
        }
    }

    // NOVO: Função para revelar as cartas durante o jogo
    private fun revealCardsDuringGame() {
        if (!isGameRunning) return

        // Desabilita todos os cliques nas cartas enquanto a animação ocorre
        cardImages.forEach { it.isClickable = false }

        // Cancela qualquer virada de carta em andamento (limpa as cartas viradas atuais)
        // Isso é importante para evitar que cartas que estavam sendo viradas quando o admin clica
        // causem bugs ou fiquem presas em um estado inconsistente.
        if (flippedCards.size == 2) { // Se duas cartas estão viradas para comparação
            val imageView1 = flippedCards.first()
            val imageView2 = flippedCards.last()
            // Vira-as de volta rapidamente antes do peek geral
            flipCardAnimation(imageView1, cardBackDrawableId, false) {}
            flipCardAnimation(imageView2, cardBackDrawableId, false) {}
        }
        flippedCards.clear() // Limpa o estado das cartas viradas
        flippedItems.clear()

        val totalCardsInLevel = levelCardCounts[currentLevel]

        // Reutiliza a função de animação de peek.
        // O callback 'onComplete' de performQuickPeekAnimation já cuida de re-habilitar os cliques.
        performQuickPeekAnimation(totalCardsInLevel, cardBackDrawableId) {
            // Este bloco será executado após a animação de peek completa (revelar e esconder)
            // e os cliques forem re-habilitados para as cartas não combinadas.
        }
    }


    // Função para adicionar 1 minuto ao timer
    private fun addMinuteToTimer() {
        if (isGameRunning && timer != null) {
            timer?.cancel() // Cancela o timer atual
            timeLeftInMillis += 60000L // Adiciona 60 segundos (60000 milissegundos)
            startTimer() // Reinicia o timer com o novo tempo
        } else {
            timeLeftInMillis += 60000L
            val seconds = timeLeftInMillis / 1000
            timerText.text = "Tempo: ${seconds}s" // Atualiza o texto mesmo sem o timer rodando
            Toast.makeText(this, "Tempo adicionado para o próximo jogo!", Toast.LENGTH_SHORT).show()
        }
    }


    private fun resetHighScores() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.remove(KEY_HIGH_SCORES)
        editor.apply()
        displayHighScores()
    }

    private fun showNameInputDialog(finalScore: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Fim de Jogo!")
        builder.setMessage("Sua pontuação final é: $finalScore.\nDigite seu nome para o placar:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Salvar") { dialog, _ ->
            var playerName = input.text.toString().trim()
            if (playerName.isEmpty()) {
                playerName = "Anônimo"
            }
            saveHighScore(playerName, finalScore)
            dialog.dismiss()
            setupEndGameUI() // Chama a função para configurar a UI de fim de jogo
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
            setupEndGameUI() // Chama a função para configurar a UI de fim de jogo mesmo se cancelar
        }

        builder.setCancelable(false)
        builder.show()
    }

    // NOVO: Função para ir direto para a Fase 3
    private fun goToPhase3() {
        if (isGameRunning) {
            // Se o jogo estiver em andamento, encerre-o primeiro
            timer?.cancel()
            backgroundMusicPlayer?.pause()
            backgroundMusicPlayer?.seekTo(0)
        }
        currentLevel = 2 // Define o nível para o índice da Fase 3 (0=Fase1, 1=Fase2, 2=Fase3)
        score = 0 // Opcional: zera a pontuação ao pular de fase para evitar pontuações inflacionadas
        startGameForCurrentLevel() // Inicia o jogo no nível 3
        consecutiveMatches = 0 // Zera o contador de acertos consecutivos ao ir para a fase 3
        Toast.makeText(this, "Pulando para a Fase 3 (20 cartas)!", Toast.LENGTH_LONG).show()
    }

    // NOVO: Diálogo de senha para ir para a Fase 3
    private fun showGoToPhase3PasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ir para Fase 3")
        builder.setMessage("Digite a senha para ir direto para a Fase 3 (20 cartas):")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Confirmar") { dialog, _ ->
            val enteredPassword = input.text.toString()
            if (enteredPassword == ADMIN_PASSWORD) {
                goToPhase3() // Chama a função para ir para a Fase 3
            } else {
                Toast.makeText(this, "Senha incorreta.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }


    // --- Gerenciamento de Ciclo de Vida do Áudio ---
    override fun onPause() {
        super.onPause()
        timer?.cancel()
        backgroundMusicPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (isGameRunning) {
            timer?.start()
            backgroundMusicPlayer?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        backgroundMusicPlayer?.stop()
        backgroundMusicPlayer?.release()
        backgroundMusicPlayer = null

        soundPool?.release()
        soundPool = null


        mediaPlayer?.release()
        mediaPlayer = null
    }
}