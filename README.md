# Adventure-digest-bot
Adventure digest bot
Выделить приглашения в ПВД среди тысячи не прочитанных сообщений в чате

как это работает, бот висит в чате и видит все сообщения, если пользователь упоминает в своем сообщение тег #pvd или
#пвд и указывает дату в формате #2024-11-07Т12:00 или #2024-12-07, то сообщение считается приглашением в ПВД и попадает
в автоматически собираемый пост-дайджест, который закреплен
таким образом приглашения не теряются среди длинных переписок

в боте присутствует разграничение прав, сообщение новичка должен подтвердить модератор
продвинутый путешественник сам может создавать сообщения в дайджест

### Описание
это telegram бот, который добавляется в основной чат где идёт обсуждение, и в канал в котором будет копировать дайджест какие анонс. 

бот видит все сообщения в чате, если попадается анонс мероприятия. то оно копируется в канал анонсов. 

### пользователи делятся на следующие группы: 
#### начинающий 
все его сообщения попадают в дайджест после модератора
#### путешественник 
может выкладывать одно сообщение в день
#### продвинутый 
может выкладывать много сообщений в течение одного дня
#### модератор 
модерирование входящих сообщений
#### админ 
может менять группу пользователей и какие-то настройки системы


в зависимости от настроек системы, новый пользователь добавляется в какую-то из существующих групп.

### этапы:
#### первый
бот просто находит по ключам нужные сообщения и копирует их в канал 
#### второй
определение событий по ключевым словам и поддержания одного поста дайджеста со всеми будущими событиями 
#### третий 
администрирование пользователя, модератор может контролировать входящие сообщения, администратор менять группы пользователей 
#### четвёртый 
...
бот собирает список тех кто едет или собирается 

бот организует список водителей и свободных мест - сложный была была кар

## Релизы

### 0.1.0 первый самостоятельный запуск, умеет сканить чатик, общается с модератором
