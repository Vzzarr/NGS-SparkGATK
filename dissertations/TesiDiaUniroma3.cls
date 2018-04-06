%%
%% Class TesiDiaUniroma3
%% by Enrico Gasperoni
%%
%% sostanzialmente un adattamento dell'ottimo lavoro di Franco Milicchio:
%%% Class phddiauniroma3
%%% PhD Thesis - Dipartimento Informatica ed Automazione
%%% Roma Tre University
%%% (C) 2006-2007, Franco Milicchio et al.
%%
%% il quale ha il vero merito e che ringrazio vivamente.
%% NOTA: tutti gli eventuali errori sono probabilmente stati introdotti da me.
%% 
%% Corretti alcuni bug:
%%
%% 1- L'intestazione dell'introduzione spariva dalla 3a pagina di introduzione
%% 2- L'intestazione della bibliografia, dalla 2a pagina in poi diventava "Conclusioni e sviluppi futuri"
%%
%% Bug Conosciuti:
%%
%% 1- La grandezza delle intestazioni può essere troppo grande per titoli dei capitoli lunghi
%% 2- Il frontespizio fa uso di grandezze del font relative, in caso venga aumentata la grandezza
%%    del font, il frontespizio potrebbe non entrare in una singola pagina, va quindi regolato di conseguenza.
%% 3- In caso di titolo della tesi lungo e su più righe, è il caso di cambiare le grandezze dei font sul
%%    frontespizio per far si che le varie parti siano più spaziate


\NeedsTeXFormat{LaTeX2e}[1995/12/01]
\ProvidesPackage{TesiDiaUniroma3}[2008/02/13 v1.0 Tesi - DIA-Uniroma3]

\LoadClass[a4paper,11pt,oneside,english]{memoir}

% Si utilizza la sillabazione italiana
\RequirePackage[english]{babel}
% Load graphicx
\RequirePackage{graphicx}
% Line spacing
\RequirePackage{setspace}

% pacchetti per i font
\usepackage[T1]{fontenc}
% interlinea
\linespread{1.4}
% margini (la dimensione del blocco di testo viene regolata di conseguenza)
\setlrmarginsandblock{3.5cm}{3cm}{*}
% profondità dell'indice (settato per stampare fino alle sottosezioni)
\setcounter{tocdepth}{2}
% numera parti, capitoli, sezioni, sottosezioni e sotto-sottosezioni
\maxsecnumdepth{subsubsection}
\setsecnumdepth{subsubsection}

% header personalizzati
\let\footruleskip\relax % for compatibility of memoir and fancyhdr
\let\rm\rmfamily        % for compatibility of memoir and blindtext (demo only)
\usepackage{fancyhdr}
\pagestyle{fancy}
\fancyhf{}
\addtolength{\headheight}{14pt}
\fancyhead[R]{\textbf{\thepage}}
\fancyhead[L]{\textbf{\leftmark}}

% AnnoAccademico (Anno/Anno+1)
\newcommand{\@annoAccademico}{2006/2007}
\newcommand{\annoAccademico}[1]{\renewcommand{\@annoAccademico}{#1}}
% Autore 
\newcommand{\@autore}{Cognome Nome}
\newcommand{\autore}[1]{\renewcommand{\@autore}{#1}}
% Titolo
\newcommand{\@titolo}{Titolo della tesi}
\newcommand{\titolo}[1]{\renewcommand{\@titolo}{#1}}
% Matricola 
\newcommand{\@matricola}{000000}
\newcommand{\matricola}[1]{\renewcommand{\@matricola}{#1}}
% Relatore (Titolo Nome Cognome, es: Prof. Pippo Caruso)
\newcommand{\@relatore}{Prof. Nome Cognome}
\newcommand{\relatore}[1]{\renewcommand{\@relatore}{#1}}
% Correlatore (Titolo Nome Cognome, es: Dott. Pippo Caruso)
\newcommand{\@correlatore}{Prof. Nome Cognome}
\newcommand{\correlatore}[1]{\renewcommand{\@correlatore}{#1}}
% Dedica
\newcommand{\@dedica}{}
\newcommand{\dedica}[1]{\renewcommand{\@dedica}{#1}}

% Generazione del FRONTESPIZIO
\newcommand{\generaFrontespizio}{
   \thispagestyle{empty}
   \begin{center}
      \vspace{10mm}
      {\includegraphics[width=0.4 \linewidth]{figure/uniroma3-logo}} \\
      \vspace{10mm}
      {\Large UNIVERSIT\`A DEGLI STUDI ROMA TRE} \\
      \vspace{5mm}
      {\LARGE Engineering Department} \\
      {\Large Master Degree Course in Ingegneria Informatica} \\
      \vspace{15mm}
      {\LARGE Thesis} \\
      \vspace{15mm}
      {\HUGE \@titolo} \\
      \vspace{15mm}
      {\large Majoring \\ \textbf{\@autore} \\ Freshman \@matricola} \\
      \vspace{7mm}
%
% scommenta questo blocco...
%
%      {\large Relatore \\ \textbf{\@relatore}} \\
%
% ... oppure scommenta questo blocco
%
      \begin{tabular}{c  @{\hspace{2.5cm}} c}
        Supervisor & Correlator \\
        \textbf{\@relatore} & \textbf{\@correlatore} \\
      \end{tabular} \\
%
      \vfill
      {\large Accademic Year \@annoAccademico} \\
   \end{center}
   \newpage 
}

% Generazione della DEDICA (posizionata ad un terzo della pagina)
\newcommand{\generaDedica}{
   \thispagestyle{empty}
   \null\vspace{\stretch{1}}
   \begin{flushright}
      \emph{{\@dedica}}
   \end{flushright}
   \vspace{\stretch{2}}\null
   \newpage
}

% Comandi riguardanti l'inserimento di capitoli
\newcommand{\ringraziamenti}[1]{\newpage \chapter*{Thanks} \input{#1}}
\newcommand{\introduzione}[1]{\newpage \chapter{Introduction} \input{#1}}
\newcommand{\conclusioni}[1]{\newpage \chapter{Conclusions and Future Developments} \markboth{CONCLUSIONI E SVILUPPI FUTURI}{CONCLUSIONI E SVILUPPI FUTURI} \input{#1}}
\newcommand{\capitolo}[2]{\newpage \chapter{#1}\label{cap:#2}\input{#2}}

% Comandi riguardanti la generazione di indici
\newcommand{\generaIndice}{
	%\fancyhead[L]{\textbf{\rightmark}}
	\newpage
	\tableofcontents
	\clearpage
	%\fancyhead[L]{\textbf{\leftmark}}
}
\newcommand{\generaIndiceTabelle}{\newpage \listoftables}
\newcommand{\generaIndiceFigure}{\newpage \listoffigures}

% Bibliografia in ordine alfabetico
\let \OldBib \bibliography
\renewcommand{\bibliography}[1]{\newpage \markboth{Bibliography}{Bibliography} \OldBib{#1}}
\bibliographystyle{alpha}
