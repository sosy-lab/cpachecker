# importing the required module
import matplotlib.pyplot as plt
import os
import datetime

def get_benchmark_dir_name_from_path(path):
    dir_name = path.split("\\")[-1]
    dir_name = dir_name[:dir_name.rindex(".")]
    return dir_name


def select_working_dir(base_dir):
    list_subfolders_with_paths = [f.path for f in os.scandir(base_dir) if f.is_dir() and f.path.split("\\")[-1].startswith("microbenchmarking") and f.path.split("\\")[-1].endswith(".files")]

    for x in range(0, len(list_subfolders_with_paths)):
        print("[" + str(x+1) + "] " + get_benchmark_dir_name_from_path(list_subfolders_with_paths[x]))

    benchmark_index = int(input("Please enter the index of the benchmark run you want to plot: "))
    return list_subfolders_with_paths[benchmark_index - 1]


def get_chart_labels(benchmark_times):
    return ["#"+str((x+1)) for x in range(0, len(benchmark_times))]   


def select_individual_runs(benchmark_dirs):
    print("Your selected benchmark has " + str(len(benchmark_dirs)) + " runs.")
    print("Please select the start and end index of the runs for which you want to generate a plot. (1 to " + str(len(benchmark_dirs)) + ")")
    start_index = int(input("Start index: ")) - 1
    end_index = int(input("End index: ")) - 1
    return start_index, end_index


def get_benchmark_data_from_file(f):
    run_data_file = os.path.join(f, "output", "output.txt")
    file_data = open(run_data_file, "r")
    file_data.readline()

    benchmark_times = file_data.readline().split(";")[:-1]
    return list(map(lambda x: int(x) / 1000000, benchmark_times))


def generate_benchmark_runs_plots(directory):
    benchmark_dirs = [f for f in os.scandir(directory) if f.is_dir()]

    generate_graphs = input("Do you want to generate graphs for individual runs? Y/n: ")

    if generate_graphs == "n":
        return

    
    start_index, end_index = select_individual_runs(benchmark_dirs)
    if start_index < 0 or end_index > len(benchmark_dirs) or start_index > end_index:
        print("User error!")
        return

    
    print("Starting graph plotting...")
    for x in range(start_index, end_index + 1):
        print("Plotting " + benchmark_dirs[x].name)
        output_file_graph = os.path.join(benchmark_dirs[x], "output", "graph.png")
        benchmark_times = get_benchmark_data_from_file(benchmark_dirs[x])
        plt.bar(
            ["#"+str((x+1)) for x in range(0, len(benchmark_times))],
            benchmark_times
        )
        mean = sum(benchmark_times) / len(benchmark_times)
        plt.axhline(mean, color='orange', linewidth=2)
        plt.xlabel("# Benchmark Run")
        plt.ylabel("Run time in milliseconds")
        plt.savefig(output_file_graph)


def get_label_for_benchmark_run(directory):
    run_data_file = os.path.join(directory, "output", "output.txt")
    file_data = open(run_data_file, "r")
    title = file_data.readline()
    return title.split("/")[-1]


def select_multi_run_indices(directories):
    print("Please select which runs you want to include in a combined graph: (1 to " + str(len(directories)) + ")")
    start_index = int(input("Start index (inclusive): "))
    end_index = int(input("End index (inclusive): "))
    return start_index, end_index


def generate_multi_run_plot(directory):
    benchmark_dirs = [f for f in os.scandir(directory) if f.is_dir()]
    start_index, end_index = select_multi_run_indices(benchmark_dirs)

    if start_index < 1 or end_index > len(benchmark_dirs) or start_index > end_index:
        print("User error!")
        return

    benchmark_dirs = benchmark_dirs[(start_index-1):end_index]
    num_included_runs = len(benchmark_dirs)

    if (len(benchmark_dirs) == 0 or num_included_runs < 1 or num_included_runs > len(benchmark_dirs)):
        print("User error!")
        return
    
    run_entry_count = len(get_benchmark_data_from_file(benchmark_dirs[0]))
    fig, ax = plt.subplots()
    parsed_runs = [get_benchmark_data_from_file(f) for f in benchmark_dirs]
    bar_width = 1
    step = (num_included_runs + 1) * bar_width
    x_values = [(x + x * step) for x in range(0, run_entry_count)]
    for y in range(0, num_included_runs):
        x_vals = [(val + y * bar_width) for val in x_values]
        # bar = ax.bar(x_vals, parsed_runs[y], bar_width, label=get_label_for_benchmark_run(selected_runs[y]))
        bar = ax.bar(x_vals, parsed_runs[y], bar_width, label=benchmark_dirs[y].name)
        ax.bar_label(bar, padding=5, fmt='%.2f', label_type='edge', fontsize=8, rotation=90)

    ax.set_ylabel("Runtime in ms")
    ax.set_title("Time per run per benchmark")
    ax.legend()
    width = num_included_runs * run_entry_count / 4
    fig.set_size_inches(width, width / 2)
    x_ticks = [(val + num_included_runs * bar_width / 2 - bar_width / 2) for val in x_values]
    ax.set_xticks(x_ticks, ["#"+str((x+1)) for x in range(0, run_entry_count)])
    output_file_graph = os.path.join(directory, "combined-graph.png")
    plt.savefig(output_file_graph, bbox_inches='tight')


if __name__ == "__main__":
    basepath = os.path.dirname(__file__)
    directory = os.path.abspath(os.path.join(basepath, "..", "test", "results"))

    selected_directory = select_working_dir(directory)
    print("Selected: " + get_benchmark_dir_name_from_path(selected_directory))
    generate_benchmark_runs_plots(selected_directory)
    generate_multi_run_plot(selected_directory)