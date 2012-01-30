#include "parserInterface.h"
#include <cstdlib>
#include <cstdio>
#include <map>
#include <string>
#include "monitor.impl"
#include <vector>
#include <stack>
#include <algorithm>
#include <iostream>

void assert_equal(unsigned state1, unsigned state2) {
	if(state1 != state2) {
		printf("ERROR: found state %i but expected state %i (UNSAFE)\n", state1, state2);
		exit(EXIT_FAILURE);
	}
}

namespace {
	class CFA {
	private:
		class Node {
		public:
			std::vector<std::pair<unsigned, std::string>> edges;
		};
		std::vector<Node> nodes;		
	public:
		unsigned create_node() {
			nodes.emplace_back(Node());
			return nodes.size()-1;
		}
		void add_edge(unsigned source, unsigned target, std::string op) {
			nodes[source].edges.push_back(std::make_pair(target, op));
		}
		unsigned get_size() {
			return nodes.size();
		}
		std::vector<std::pair<unsigned, std::string>> get_edges(unsigned node_id) {
			return nodes[node_id].edges;				
		}
	};
}

CFA cfa;

void traverse_cfa(unsigned root, const char* function) {
	if(cfa.get_edges(root).empty()) {
		return; //empty function -> ignore
	}

	std::vector<unsigned> node_to_state;
	node_to_state.resize(cfa.get_size());
	std::transform(begin(node_to_state), end(node_to_state), begin(node_to_state), [](unsigned){return -1;});

	std::stack<unsigned> unhandled_nodes;
	unhandled_nodes.push(0); //root is 0
	node_to_state[0] = get_initial_state();
	while(!unhandled_nodes.empty()) {
		unsigned unhandled_node = unhandled_nodes.top();
		unhandled_nodes.pop();

		for(auto pair : cfa.get_edges(unhandled_node)) {
			unsigned new_state = compute_successor(node_to_state[unhandled_node], pair.second);
			if(node_to_state[pair.first] == -1) {
				node_to_state[pair.first] = new_state;
				unhandled_nodes.push(pair.first);
			} else {
				assert_equal(node_to_state[pair.first], new_state);
			}
		}
	}
	std::cout << "Function " << function << " is safe." << std::endl;
}

unsigned new_node() { 
	return cfa.create_node(); 
}

std::map<std::string, unsigned> label_to_node;

void register_node(const char* label, unsigned node) {
	label_to_node[label] = node;
}

unsigned resolve_label(const char* label) { 
	return label_to_node[label];
}

void create_edge(unsigned source, unsigned target, const char* op) {
	cfa.add_edge(source, target, op);
}

