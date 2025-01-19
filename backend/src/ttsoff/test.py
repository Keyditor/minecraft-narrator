import threading
from edgeTTS import fala

threading.Thread(
                target=fala,
                kwargs={
                    "text": "1, 2, 3, indiozinhos! 4, 5, 6, indiozinhos!",
                },
            ).start()
