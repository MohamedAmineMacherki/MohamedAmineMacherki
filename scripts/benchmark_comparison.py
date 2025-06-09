#!/usr/bin/env python3
"""
Benchmark comparison script for MCTS vs HSP planners
@author: StrongAmineMohamed
"""

import subprocess
import json
import time
import os
import sys
import matplotlib.pyplot as plt
import pandas as pd
from pathlib import Path
import argparse
import re

class PlannerBenchmark:
    def __init__(self, pddl4j_path, timeout=300):
        self.pddl4j_path = pddl4j_path
        self.timeout = timeout
        self.results = []
        
    def run_hsp_planner(self, domain_file, problem_file):
        cmd = [
            "java", "-cp", self.pddl4j_path, 
            "fr.uga.pddl4j.planners.statespace.HSP",
            "-d", domain_file,
            "-p", problem_file,
            "-t", str(self.timeout)
        ]
        
        start_time = time.time()
        try:
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=self.timeout)
            end_time = time.time()
            
            runtime = end_time - start_time
            plan_length = self.extract_plan_length(result.stdout)
            success = result.returncode == 0 and plan_length > 0
            
            return {
                'success': success,
                'runtime': runtime,
                'plan_length': plan_length,
                'stdout': result.stdout,
                'stderr': result.stderr
            }
            
        except subprocess.TimeoutExpired:
            return {
                'success': False,
                'runtime': self.timeout,
                'plan_length': -1,
                'stdout': '',
                'stderr': 'Timeout'
            }
    
    def run_mcts_planner(self, domain_file, problem_file, walks=1000, length=100):
        cmd = [
            "java", "-cp", f"{self.pddl4j_path}:target/classes",
            "fr.uga.pddl4j.planners.mcts.MCTSPlanner",
            "-d", domain_file,
            "-p", problem_file,
            "-t", str(self.timeout),
            "-w", str(walks),
            "-l", str(length)
        ]
        
        start_time = time.time()
        try:
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=self.timeout)
            end_time = time.time()
            
            runtime = end_time - start_time
            plan_length = self.extract_plan_length(result.stdout)
            success = result.returncode == 0 and plan_length > 0
            
            return {
                'success': success,
                'runtime': runtime,
                'plan_length': plan_length,
                'stdout': result.stdout,
                'stderr': result.stderr
            }
            
        except subprocess.TimeoutExpired:
            return {
                'success': False,
                'runtime': self.timeout,
                'plan_length': -1,
                'stdout': '',
                'stderr': 'Timeout'
            }
    
    def extract_plan_length(self, output):
        patterns = [
            r'Plan length:\s*(\d+)',
            r'Solution found with (\d+) actions',
            r'(\d+) actions in the plan',
            r'Steps:\s*(\d+)'
        ]
        
        for pattern in patterns:
            match = re.search(pattern, output)
            if match:
                return int(match.group(1))
        
        action_count = len([line for line in output.split('\n') 
                           if line.strip().startswith('(') and ')' in line])
        return action_count if action_count > 0 else -1
    
    def save_results(self, filename="benchmark_results.json"):
        with open(filename, 'w') as f:
            json.dump(self.results, f, indent=2)
        print(f"Results saved to {filename}")
    
    def generate_plots(self):
        if not self.results:
            print("No results to plot")
            return
        
        df = pd.DataFrame(self.results)
        domains = df['domain'].unique()
        
        os.makedirs('results/figures', exist_ok=True)
        
        for domain in domains:
            domain_df = df[df['domain'] == domain].copy()
            domain_df = domain_df.sort_values('hsp_runtime')
            
            plt.figure(figsize=(12, 6))
            x = range(len(domain_df))
            
            plt.subplot(1, 2, 1)
            plt.plot(x, domain_df['hsp_runtime'], 'bo-', label='HSP', linewidth=2)
            plt.plot(x, domain_df['mcts_runtime'], 'ro-', label='MCTS', linewidth=2)
            plt.xlabel('Problems (ordered by HSP difficulty)')
            plt.ylabel('Runtime (seconds)')
            plt.title(f'{domain.capitalize()} - Runtime Comparison')
            plt.legend()
            plt.grid(True, alpha=0.3)
            
            plt.subplot(1, 2, 2)
            hsp_lengths = [l if l > 0 else float('inf') for l in domain_df['hsp_plan_length']]
            mcts_lengths = [l if l > 0 else float('inf') for l in domain_df['mcts_plan_length']]
            
            plt.plot(x, hsp_lengths, 'bo-', label='HSP', linewidth=2)
            plt.plot(x, mcts_lengths, 'ro-', label='MCTS', linewidth=2)
            plt.xlabel('Problems (ordered by HSP difficulty)')
            plt.ylabel('Plan Length')
            plt.title(f'{domain.capitalize()} - Plan Length Comparison')
            plt.legend()
            plt.grid(True, alpha=0.3)
            
            plt.tight_layout()
            plt.savefig(f'results/figures/{domain}_comparison.png', dpi=300, bbox_inches='tight')
            plt.close()

def main():
    parser = argparse.ArgumentParser(description='Benchmark MCTS vs HSP planners')
    parser.add_argument('--pddl4j-path', required=True, 
                       help='Path to PDDL4J jar file')
    parser.add_argument('--domains-path', default='pddl',
                       help='Path to PDDL domains directory')
    parser.add_argument('--timeout', type=int, default=300,
                       help='Timeout in seconds (default: 300)')
    
    args = parser.parse_args()
    benchmark = PlannerBenchmark(args.pddl4j_path, args.timeout)
    benchmark.save_results()
    benchmark.generate_plots()
    print("Benchmark completed!")

if __name__ == "__main__":
    main()
