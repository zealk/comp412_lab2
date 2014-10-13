#!/usr/bin/ruby
# Skeleton Parser for a LL(1) table driven parser.
# Algorithm from Figure 3.11 in Engineering a Compiler Chapter3Revised.pdf (pg. 119)
#
# Dave Peixotto (dmp@rice.edu)
# 30 Oct 2008
#

require 'yaml'
require 'optparse'
require 'pp'


# add a top method to Array objects
class Array
  alias :top :last
end

# trace the actions of the parser
class Trace
  def self.trace(rule, production, stack, input)
    if not $trace then return end
    str = ""
    PP::singleline_pp(rule, str)
    if production then
      str << " : " 
      PP::pp(production, str)
    else
      str << "\n"
    end
    str << "stack: "
    PP::pp(stack.reverse, str)
    str << "input: "
    PP::pp(input.words, str)
    puts str
    puts
  end
  def self.output(str)
    puts(str) if $trace
  end
end

#simple lexer for reading parser input
class Lexer
  attr_accessor :words
  def initialize(input, eof)
    @words = input.read.split(/\s/) << eof #eagerly read input
  end
  
  def nextWord
    @words.shift
  end
end

class SkeletonParser
  def initialize(terminals, nonterminals, eof, error_marker, start, productions, table)
    @terminals = terminals
    @nonterminals = nonterminals
    @eof = eof
    @error_marker = error_marker
    @start = start
    @productions = productions
    @table = table
  end
  
  def error(msg, stack, lexer)
    puts "ERROR: #{msg}"
    pp stack.reverse
    pp lexer
    exit 2
  end
  
  # Skeleton Parser algorithm from Figure 3.11
  def run(lexer)
    stack = [@eof, @start] #stack goes [bottom,...,top]
    Trace::trace("start", nil, stack, lexer)
    focus = stack.top
    word  = lexer.nextWord

    #main parser loop
    loop do
      Trace::output "FOCUS: #{focus}"
      Trace::output " WORD: #{word}"
      #successful parse
      if focus == @eof && word == @eof then
        puts "SUCCESS"
        break      
      #terminal recognized
      elsif @terminals.member?(focus) || focus == @eof then
        if focus == word then        
          stack.pop
          word = lexer.nextWord
          Trace::trace("focus match, popping #{focus}", nil, stack, lexer)        
        else
          error("TOS does not match current word", stack, lexer)
        end
      #nonterminal recognized
      else
        production_number = @table[focus][word]
        production = @productions[production_number]
        if production && production != @error_marker then
          stack.pop
          lhs,rhs = production.entries.first
          rhs.reverse.each {|symbol| stack << symbol} 
          Trace::trace("taking production #{production_number}", production, stack, lexer)
        else
          error("error expanding [#{focus}, #{word}] = #{production_number}: #{production}", stack, lexer)
        end
      end
      focus = stack.top    
    end
  end
end

#main
if __FILE__ == $0 then
  options = {:trace => false, :defs => 'llgen.yaml'}
   OptionParser.new do |opts|
     opts.banner = "Usage: skeletonParser.rb -d DEFINITION_FILE [-t] [parser input file]"
     opts.on("-t", "--[no-]trace", "Trace Parser Actions") do |t|
       options[:trace] = t
     end
     opts.on("-d", "--definition-file FILE", "Parser Table Definitions") do |d|
        options[:defs] = d
     end     
   end.parse!
  
  $trace = options[:trace]
  llgen  = YAML::load(File.open(options[:defs]))
  input  = if ARGV.empty? then $stdin else File.open(ARGV.first) end
  
  #read tables from llgen
  terminals = llgen["terminals"]
  nonterminals = llgen["non-terminals"]
  eof = llgen["eof-marker"]
  error_marker = llgen["error-marker"]
  start = llgen["start-symbol"]
  productions = llgen["productions"]
  table = llgen["table"]
  
  #initialize lexer
  lexer = Lexer.new(input, eof)
  parser = SkeletonParser.new(terminals, nonterminals, eof, error_marker, start, productions, table)
  parser.run(lexer)
end

#Example Grammar Definitions for the Expr grammar (right recursive)
__END__
terminals:
  - +
  - -
  - x
  - /
  - (
  - )
  - name
  - num
non-terminals:
  - Expr
  - Expr'
  - Term
  - Term'
  - Factor
eof-marker: <EOF>
error-marker: --
start-symbol: Expr

productions:
  0: {Goal: [Expr]}
  1: {Expr: [Term, Expr']}
  2: {Expr': [+, Term, Expr']}
  3: {Expr': [-, Term, Expr']}
  4: {Expr': []}
  5: {Term: [Factor, Term']}
  6: {Term': [x, Factor, Term']}
  7: {Term': [/, Factor, Term']}
  8: {Term': []}
  9: {Factor: [(, Expr, )]}
  10: {Factor: [num]}
  11: {Factor: [name]}
    
table:
  Expr: {+: --, -: --, x: --, /: --, (: 1, ): --, name: 1, num: 1, <EOF>: -- }
  Expr': {+: 2, -: 3, x: --, /: --, (: --, ): 4, name: --, num: --, <EOF>: 4 }
  Term: {+: --, -: --, x: --, /: --, (: 5, ): --, name: 5, num: 5, <EOF>: -- }
  Term': {+: 8, -: 8, x: 6, /: 7, (: --, ): 8, name: --, num: --, <EOF>: 8 }
  Factor: {+: --, -: --, x: --, /: --, (: 9, ): --, name: 11, num: 10, <EOF>: -- }
