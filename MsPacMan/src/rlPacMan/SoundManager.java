package rlPacMan;
import java.applet.*;
import java.net.URL;

//Loads and holds a bunch of audio files whose locations are specified
//relative to a fixed base URL.
class SoundManager {
	// Pacman Sounds
	AudioClip m_chompClip;
	AudioClip m_eatGhostClip;
	AudioClip m_pacmanDiesClip;
	AudioClip m_returnGhostClip;
	AudioClip m_sirenClip;
	AudioClip m_startClip;
	AudioClip m_ghostBlueClip;
	AudioClip m_extraPacClip;
	AudioClip m_eatFruitClip;

	static final int SOUND_PACMANDIES_LENGTH = 1605;
	static final int SOUND_CHOMP_LENGTH = 243;
	static final int SOUND_RETURNGHOST_LENGTH = 705;
	static final int SOUND_START_LENGTH = 4630;

	static final int SOUND_CHOMP = 1;
	static final int SOUND_EATGHOST = 2;
	static final int SOUND_PACMANDIES = 3;
	static final int SOUND_RETURNGHOST = 4;
	static final int SOUND_SIREN = 5;
	static final int SOUND_START = 6;
	static final int SOUND_GHOSTBLUE = 7;
	static final int SOUND_EXTRAPAC = 8;
	static final int SOUND_EATFRUIT = 9;

	PacMan m_pacMan;
	URL m_baseURL;
	boolean m_bLoaded = false;
	int m_nChompTicks = 0;
	boolean m_bChompLooping = false;
	boolean m_bSirenLooping = false;
	int m_nReturnGhostTicks = 0;
	boolean m_bEnabled = true;

	public SoundManager(PacMan pacMan, URL baseURL) {
		m_pacMan = pacMan;
		m_baseURL = baseURL;
	}

	public void loadSoundClips() {
		if (m_baseURL != null) {
			long beginLoadTime = System.currentTimeMillis();

			/*m_chompClip = m_pacMan.getAudioClip(m_baseURL,
					"../resource/gs_chomp.au");
			m_eatGhostClip = m_pacMan.getAudioClip(m_baseURL,
					"../resource/gs_eatghost.au");
			m_pacmanDiesClip = m_pacMan.getAudioClip(m_baseURL,
					"../resource/gs_pacmandies.au");
			m_returnGhostClip = m_pacMan.getAudioClip(m_baseURL,
					"../resource/gs_returnghost.au");
			m_sirenClip = m_pacMan.getAudioClip(m_baseURL,
					"../resource/gs_siren_soft.au");
			m_startClip = m_pacMan.getAudioClip(m_baseURL,
					"../resource/gs_start.au");
			m_ghostBlueClip = m_pacMan.getAudioClip(m_baseURL,
					"../resource/gs_ghostblue.au");
			m_extraPacClip = m_pacMan.getAudioClip(m_baseURL,
					"../resource/gs_extrapac.au");
			m_eatFruitClip = m_pacMan.getAudioClip(m_baseURL,
					"../resource/gs_eatfruit.au");*/
			m_bLoaded = true;

			long endLoadTime = System.currentTimeMillis();
			System.out.println(endLoadTime - beginLoadTime);
		}
	}

	public void tickSound() {
		if (m_nChompTicks > 0) {
			m_nChompTicks--;
			if (m_nChompTicks == 0) {
				m_bChompLooping = false;
				stopSound(SOUND_CHOMP);
			}
		}
		if (m_nReturnGhostTicks > 0) {
			m_nReturnGhostTicks--;
			if (m_nReturnGhostTicks == 0) {
				stopSound(SOUND_RETURNGHOST);
			}
		}
	}

	// Public method exposed for other classes to play
	// various sounds.
	public void playSound(int soundEnum) {
		if (!m_bLoaded || !m_bEnabled)
			return;

		switch (soundEnum) {
		case SOUND_CHOMP:
			m_nChompTicks = SOUND_CHOMP_LENGTH / m_pacMan.m_delay; // Length of
			// this clip
			// in Ticks
			if (!m_bChompLooping) {
				m_chompClip.loop();
				m_bChompLooping = true;
			}
			break;

		case SOUND_EATGHOST:
			m_eatGhostClip.play();
			break;

		case SOUND_PACMANDIES:
			m_pacmanDiesClip.play();
			break;

		case SOUND_RETURNGHOST:
			m_nReturnGhostTicks = (SOUND_RETURNGHOST_LENGTH / m_pacMan.m_delay) * 2;
			m_returnGhostClip.loop();
			break;

		case SOUND_SIREN:
			if (!m_bSirenLooping) {
				m_sirenClip.loop();
				m_bSirenLooping = true;
			}
			break;

		case SOUND_START:
			m_startClip.play();
			break;

		case SOUND_GHOSTBLUE:
			m_ghostBlueClip.loop();
			break;

		case SOUND_EXTRAPAC:
			m_extraPacClip.play();
			break;

		case SOUND_EATFRUIT:
			m_eatFruitClip.play();
			break;

		}
	}

	// Public method exposed for other classes to stop
	// any sound whether it is playing or looping
	public void stopSound(int soundEnum) {
		if (!m_bLoaded)
			return;

		switch (soundEnum) {
		case SOUND_CHOMP:
			m_chompClip.stop();
			m_nChompTicks = 0;
			m_bChompLooping = false;
			break;

		case SOUND_EATGHOST:
			m_eatGhostClip.stop();
			break;

		case SOUND_PACMANDIES:
			m_pacmanDiesClip.stop();
			break;

		case SOUND_RETURNGHOST:
			m_returnGhostClip.stop();
			m_nReturnGhostTicks = 0;
			break;

		case SOUND_SIREN:
			m_sirenClip.stop();
			m_bSirenLooping = false;
			break;

		case SOUND_START:
			m_startClip.stop();
			break;

		case SOUND_GHOSTBLUE:
			m_ghostBlueClip.stop();
			break;

		case SOUND_EXTRAPAC:
			m_extraPacClip.stop();
			break;

		case SOUND_EATFRUIT:
			m_eatFruitClip.stop();
			break;
		}
	}

	public void stop() {
		if (!m_bLoaded)
			return;

		m_chompClip.stop();
		m_eatGhostClip.stop();
		m_pacmanDiesClip.stop();
		m_returnGhostClip.stop();
		m_sirenClip.stop();
		m_startClip.stop();
		m_ghostBlueClip.stop();
		m_extraPacClip.stop();
		m_eatFruitClip.stop();
		m_nChompTicks = 0;
		m_bChompLooping = false;
		m_bSirenLooping = false;
		m_nReturnGhostTicks = 0;
	}
}
