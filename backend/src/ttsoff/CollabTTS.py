import requests
import sounddevice as sd
import wave
import numpy as np
import os

# Função para tocar áudio de um arquivo WAV
def play_audio(file_path):
    # Abre o arquivo WAV
    with wave.open(file_path, 'rb') as wf:
        fs = wf.getframerate()  # Taxa de amostragem
        frames = wf.readframes(wf.getnframes())  # Lê todos os quadros do áudio
        audio_data = np.frombuffer(frames, dtype=np.int16)  # Converte os dados para o formato adequado

    # Reproduz o áudio
    sd.play(audio_data, fs)
    sd.wait()  # Aguarda até o áudio terminar de tocar

# Configurações
url = "https://99bf-34-147-29-135.ngrok-free.app/synthesize"  # Substitua pelo URL gerado pelo ngrok
text = 'Essas frases são criadas para cobrir todos os fonemas do português brasileiro, incluindo os mais raros, como o som do "lh", "j", "qu" e outros. Se quiser algo mais personalizado ou com um tema específico, é só pedir!'
speaker_wav_path = "./src/ttsoff/"  # Caminho para o arquivo speaker_wav no seu computador

# Fazer o upload do texto e do arquivo speaker_wav
def Cfala(text,voice,url):

    global speaker_wav_path
    url += "/synthesize"
    speaker_wav_path = "./src/ttsoff/"+str(voice)+".wav"
    with open(speaker_wav_path, 'rb') as f:
        files = {
            'speaker_wav': f
        }
        data = {
            'text': text
        }
        response = requests.post(url, data=data, files=files)

    # Verificar a resposta
    if response.status_code == 200:
        # Salvar o arquivo de áudio gerado
        output_file = "output.wav"
        with open(output_file, "wb") as f:
            f.write(response.content)
        print("Áudio salvo como output.wav")
        play_audio(output_file)
        os.remove(output_file)
    else:
        print(f"Erro: {response.json()}")
    return False