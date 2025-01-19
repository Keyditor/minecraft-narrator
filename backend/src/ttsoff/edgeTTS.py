import os
import sounddevice as sd
import numpy as np
import wave
import torch
from TTS.api import TTS
counter = 0

# Configurações
url = "https://99bf-34-147-29-135.ngrok-free.app/synthesize"  # Substitua pelo URL gerado pelo ngrok
text = 'Essas frases são criadas para cobrir todos os fonemas do português brasileiro, incluindo os mais raros, como o som do "lh", "j", "qu" e outros. Se quiser algo mais personalizado ou com um tema específico, é só pedir!'
speaker_wav_path = "./src/ttsoff/Brian3.wav"  # Caminho para o arquivo speaker_wav no seu computador



# Função para verificar se o arquivo já existe e gerar um nome único
def get_unique_filename(base_filename):
    global counter
    filename, extension = os.path.splitext(base_filename)
    # Enquanto o arquivo já existir, incrementa o número

    base_filename = f"{filename}{counter}{extension}"
    counter += 1
    return base_filename


# Função para reproduzir o áudio usando sounddevice
def play_audio(file_path):
    # Abre o arquivo de áudio WAV
    with wave.open(file_path, 'rb') as wf:
        # Lê os dados de áudio
        fs = wf.getframerate()  # Taxa de amostragem (frequência)
        frames = wf.readframes(wf.getnframes())  # Lê todos os quadros
        audio_data = np.frombuffer(frames, dtype=np.int16)  # Converte os dados para o formato adequado

    # Reproduz o áudio
    sd.play(audio_data, fs)
    sd.wait()  # Aguarda até o áudio terminar de tocar


# Função principal
def fala(text,voice, model_name = "tts_models/multilingual/multi-dataset/xtts_v2"):
    global tts

    # Arquivo de saída
    output_file = "../output.wav"

    # Verificar se o arquivo já existe e gerar um nome único
    output_file = get_unique_filename(output_file)

    # Gerar o áudio e salvar com o locutor selecionado
    if model_name == "tts_models/multilingual/multi-dataset/xtts_v2":
        tts.tts_to_file(text=text, file_path=output_file, speaker_wav="./src/ttsoff/"+str(voice)+".wav", language="pt")
    else:
        tts = TTS(model_name=model_name)
        tts.tts_to_file(text=text, file_path=output_file)
    print(f"TTS salvo em: {output_file}")

    # Reproduzir o áudio
    play_audio(output_file)

    # Apagar o arquivo após a reprodução
    os.remove(output_file)
    print(f"Arquivo {output_file} excluído.")

    return False

# Chamar a função principal
#if __name__ == "__main__":
# Verificar se o CUDA está disponível ou fazer fallback para a CPU
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print(f"Usando dispositivo: {device}")

# Texto que será convertido para fala
# text = "... e então com pavor ele disse: Não, por favor! Não a machuque! Apenas deixe nos ir embora! Prometo tomar leite quente!"

# Modelo do Coqui TTS a ser usado (garanta que o modelo esteja disponível localmente ou será baixado automaticamente)
model_name = "tts_models/multilingual/multi-dataset/xtts_v2"  # Modelo de fala

# Inicializar o modelo
tts = TTS(model_name=model_name)

fala("Narrador inicializado com sucesso!","Brian3")