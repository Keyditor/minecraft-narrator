import random
import threading

from src.cooldown import CooldownManager
from src.models import Config, IncomingEvent, OutgoingAction, Action
from src.queue import Queue
from src.config import global_config
from src.websocket import ws
from src.chatgpt import chat
from src.tts import tts


class EventHandler:
    def __init__(self):
        self._cd_manager = CooldownManager()
        self._queue = Queue()

    def handle_cooldowns_and_queue(self, event: IncomingEvent) -> OutgoingAction:
        self._queue.put(event.data)

        if self._cd_manager.check_all_cooldown(event.event):
            return OutgoingAction(
                action=Action.IGNORE,
                data="Aguardando cooldown",
            )

        outgoing_action = OutgoingAction(
            action=Action.SEND_CHAT,
            data="\n".join(self._queue.all()),
        )
        print(self._queue.all())
        self._queue.clear()

        self._cd_manager.add_cooldown(
            event.event,
            global_config.cooldown_individual * 60,
        )  # Individual cd, 5 min

        self._cd_manager.add_cooldown(
            "GLOBAL_COOLDOWN", global_config.cooldown_global + random.randint(0, 30)
        )  # Global cd, 30 sec to 1 min

        return outgoing_action

    async def handle_game_event(self, event: IncomingEvent) -> None:
        outgoing = self.handle_cooldowns_and_queue(event)

        if outgoing.action == Action.IGNORE:
            await ws.broadcast(outgoing.model_dump())
            return

        gpt_response_generator = chat.ask(outgoing.data)
        threading.Thread(target=tts.synthesize, kwargs={"gen": gpt_response_generator}).start()

    def handle_config_event(self, req_config: Config):
        global_config.set_all(req_config)
        chat.set_config(global_config)
        tts.set_config(global_config)
        global_config.save()

event_handler = EventHandler()
